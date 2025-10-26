/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.CatalogDatabaseImpl;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.table.catalog.exceptions.DatabaseAlreadyExistException;
import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.sink.FlinkSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kickstarter.model.AirlineJsonData;


/**
 * This class creates fake flight data for two fictional airlines <b>Sunset Air</b> and 
 * <b>Sky One</b> Airlines</b>.  Then sinks the data to Kafka Topics and Apache Iceberg
 * Tables, respectively.
 */
public class JsonDataGeneratorApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDataGeneratorApp.class);


	/**
	 * The main method in a Flink application serves as the entry point of the program, where
	 * the Flink DAG is defined.  That is, the execution environment, the creation of the data
	 * streams or datasets, apply transformations, and trigger the execution of the application (by
	 * sending it to the Flink JobManager).
	 * 
	 * @param args list of strings passed to the main method from the command line.
	 * @throws Exception - The exceptions are forwarded, and are caught by the runtime.  
     * When the runtime catches an exception, it aborts the task and lets the fail-over logic
	 * decide whether to retry the task execution.
	 */
	public static void main(String[] args) throws Exception {
        // --- Retrieve the value(s) from the command line argument(s).
        String serviceAccountUser = System.getenv().getOrDefault("SERVICE_ACCOUNT_USER", Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER));
        String awsRegion = System.getenv().getOrDefault("AWS_REGION", Common.getAppArgumentValue(args, Common.ARG_AWS_REGION));
        String bucketName = System.getenv("AWS_S3_BUCKET");

        // --- Validate required configurations
        if (serviceAccountUser == null || serviceAccountUser.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Service Account User is required. Set SERVICE_ACCOUNT_USER environment variable or use --service-account-user argument."
            );
        }

        if (awsRegion == null || awsRegion.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "AWS Region is required. Set AWS_REGION environment variable or use --aws-region argument."
            );
        }

        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "S3 Bucket is required. Set AWS_S3_BUCKET environment variable."
            );
        }

        LOGGER.info("Configuration loaded - Service Account User: {}, AWS Region: {}, S3 Bucket: {}", serviceAccountUser, awsRegion, bucketName);

		// --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG).
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        /*
         * Enable checkpointing every 10,000 milliseconds (10 seconds).  Note, consider the
         * resource cost of checkpointing frequency, as short intervals can lead to higher
         * I/O and CPU overhead.  Proper tuning of checkpoint intervals depends on the
         * state size, latency requirements, and resource constraints.
         */
        env.enableCheckpointing(10000);

        // --- Set minimum pause between checkpoints to 5,000 milliseconds (5 seconds).
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5000);

        // --- Set tolerable checkpoint failure number to 3.
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(3);

        /*
         * Externalized Checkpoint Retention: RETAIN_ON_CANCELLATION" means that the system will keep the
         * checkpoint data in persistent storage even if the job is manually canceled. This allows you to
         * later restore the job from that last saved state, which is different from the default behavior,
         * where checkpoints are deleted on cancellation. This setting requires you to manually clean up
         * the checkpoint state later if it's no longer needed. 
         */
        env.getCheckpointConfig().setExternalizedCheckpointRetention(ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION);

        /*
         * Set checkpoint timeout to 60 seconds, which is the maximum amount of time a
         * checkpoint attempt can take before being discarded.  Note, setting an appropriate
         * checkpoint timeout helps maintain a balance between achieving exactly-once semantics
         * and avoiding excessive delays that can impact real-time stream processing performance.
         */
        env.getCheckpointConfig().setCheckpointTimeout(60000);

        /*
         * Set the maximum number of concurrent checkpoints to 1 (i.e., only one checkpoint
         * is created at a time).  Note, this is useful for limiting resource usage and
         * ensuring checkpoints do not interfere with each other, but may impact throughput
         * if checkpointing is slow.  Adjust this setting based on the nature of your job,
         * the size of the state, and available resources. If your environment has enough
         * resources and you want to ensure faster recovery, you could increase the limit
         * to allow multiple concurrent checkpoints.
         */
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);

        // --- Create a StreamTableEnvironment
        EnvironmentSettings settings = 
            EnvironmentSettings.newInstance()
                               .inStreamingMode()
                               .build();
        StreamTableEnvironment tblEnv = StreamTableEnvironment.create(env, settings);

		// --- Kafka Producer Client Properties.
        Properties producerProperties = Common.collectConfluentProperties(env, serviceAccountUser, false);

        // --- Create a data generator source.
        DataGeneratorSource<AirlineJsonData> skyOneSource =
            new DataGeneratorSource<>(
                index -> DataGenerator.generateAirlineJsonData("SKY1"),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(1),
                Types.POJO(AirlineJsonData.class)
            );

        // --- Sets up a Flink POJO source to consume data.
        DataStream<AirlineJsonData> skyOneStream = 
            env.fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `skyone` with the
         * specified serializer.
         */
        KafkaRecordSerializationSchema<AirlineJsonData> skyOneSerializer = 
            KafkaRecordSerializationSchema.<AirlineJsonData>builder()
                .setTopic("skyone")
                .setValueSerializationSchema(new JsonSerializationSchema<>(Common::getMapper))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG).
         */
        producerProperties.put("transaction.timeout.ms", "900000"); // 15m for long checkpoints
        producerProperties.put("enable.idempotence", "true");       // typically implied, explicit is fine
        KafkaSink<AirlineJsonData> skyOneSink = 
            KafkaSink.<AirlineJsonData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(skyOneSerializer)
                .setTransactionalIdPrefix("json-data-skyone-") // apply unique prefix to prevent backchannel conflicts and potential memory leaks
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .build();

        /*
         * Adds the given Sink to the DAG. Note only streams with sinks added will be executed
         * once the StreamExecutionEnvironment.execute() method is called.
         */
        skyOneStream.sinkTo(skyOneSink).name("skyone_sink");

        // --- Sets up a Flink POJO source to consume data.
        DataGeneratorSource<AirlineJsonData> sunsetSource =
            new DataGeneratorSource<>(
                index -> DataGenerator.generateAirlineJsonData("SUN"),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(1),
                Types.POJO(AirlineJsonData.class)
            );

        DataStream<AirlineJsonData> sunsetStream = 
            env.fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `sunset` with the
         * specified serializer.
         */
        KafkaRecordSerializationSchema<AirlineJsonData> sunsetSerializer = 
            KafkaRecordSerializationSchema.<AirlineJsonData>builder()
                .setTopic("sunset")
                .setValueSerializationSchema(new JsonSerializationSchema<>(Common::getMapper))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG).
         */
        KafkaSink<AirlineJsonData> sunsetSink = 
            KafkaSink.<AirlineJsonData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(sunsetSerializer)
                .setTransactionalIdPrefix("json-data-sunset-") // apply unique prefix to prevent backchannel conflicts and potential memory leaks
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .build();

        /*
         * Adds the given Sink to the DAG. Note only streams with sinks added will be executed
         * once the StreamExecutionEnvironment.execute() method is called.
         */
        sunsetStream.sinkTo(sunsetSink).name("sunset_sink");

        // --- Describes and configures the catalog for the Table API and SQL.
        String catalogName = "apache_kickstarter";
        String catalogImpl = "org.apache.iceberg.aws.glue.GlueCatalog";
        String databaseName = "airlines";
        Map<String, String> catalogProperties = new HashMap<>();
        catalogProperties.put("type", "iceberg");
        catalogProperties.put("warehouse", bucketName);
        catalogProperties.put("catalog-impl", catalogImpl);
        catalogProperties.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
        catalogProperties.put("glue.skip-archive", "true");
        catalogProperties.put("glue.region", awsRegion);
        CatalogDescriptor catalogDescriptor = CatalogDescriptor.of(catalogName, org.apache.flink.configuration.Configuration.fromMap(catalogProperties));

        // --- Create the catalog, use it, and get the instantiated catalog.
        tblEnv.createCatalog(catalogName, catalogDescriptor);
        tblEnv.useCatalog(catalogName);
        org.apache.flink.table.catalog.Catalog catalog = tblEnv.getCatalog(catalogName).orElseThrow(() -> new RuntimeException("Catalog not found"));

        LOGGER.info("Current catalog: {}", tblEnv.getCurrentCatalog());

        // --- Check if the database exists.  If not, create it.
        try {
            if (!catalog.databaseExists(databaseName)) {
                catalog.createDatabase(databaseName, new CatalogDatabaseImpl(new HashMap<>(), "The Airlines flight data database."), false);
            }
            tblEnv.useDatabase(databaseName);
        } catch (CatalogException | DatabaseAlreadyExistException e) {
            LOGGER.error("A critical error occurred during the processing of the database: ", e);
            throw new RuntimeException("A critical error occurred during the processing of the database", e);
        }

        // --- Print the current database name.
        LOGGER.info("Current database: {}", tblEnv.getCurrentDatabase());

        // --- Define the RowType for the RowData.
        RowType rowType = RowType.of(
            new LogicalType[] {
                DataTypes.STRING().getLogicalType(),
                DataTypes.STRING().getLogicalType(),
                DataTypes.STRING().getLogicalType(),
                DataTypes.STRING().getLogicalType(),
                DataTypes.STRING().getLogicalType(),
                DataTypes.BIGINT().getLogicalType(),
                DataTypes.STRING().getLogicalType(),
                DataTypes.STRING().getLogicalType(),
                DataTypes.DECIMAL(10, 2).getLogicalType(),
                DataTypes.STRING().getLogicalType(),
                DataTypes.STRING().getLogicalType()
            },
            new String[] {
                "email_address",
                "departure_time",
                "departure_airport_code",
                "arrival_time",
                "arrival_airport_code",
                "flight_duration",
                "flight_number",
                "confirmation_code",
                "ticket_price",
                "aircraft",
                "booking_agency_email"
            }
        );
        
        // --- Use the CatalogLoader since AWS Glue Catalog is used as the external metastore.
        CatalogLoader catalogLoader = CatalogLoader.custom(catalogName, catalogProperties,  new Configuration(false), catalogImpl);

        // --- Sink the datastreams to their respective Apache Iceberg tables.
        sinkToIcebergTable(tblEnv, catalog, catalogLoader, databaseName, rowType.getFieldCount(), "skyone_airline", skyOneStream);
        sinkToIcebergTable(tblEnv, catalog, catalogLoader, databaseName, rowType.getFieldCount(), "sunset_airline", sunsetStream);

        // --- Execute the Flink job graph (DAG)
        try {            
            env.execute("JsonDataGeneratorApp");
        } catch (Exception e) {
            LOGGER.error("The App stopped early due to the following: ", e);
            throw e;
        }
	}

    /**
     * This method is used to sink the data from the input data stream into the iceberg table.
     * 
     * @param tblEnv The StreamTableEnvironment.
     * @param catalog The Catalog. 
     * @param catalogLoader The CatalogLoader.
     * @param databaseName  The name of the database.
     * @param fieldCount The number of fields in the input data stream.
     * @param tableName The name of the table. 
     * @param airlineDataStream The input data stream.
     */
    private static void sinkToIcebergTable(final StreamTableEnvironment tblEnv, final org.apache.flink.table.catalog.Catalog catalog, final CatalogLoader catalogLoader, final String databaseName, final int fieldCount, final String tableName, DataStream<AirlineJsonData> airlineDataStream) {
        // --- Convert DataStream<AirlineJsonData> to DataStream<RowData>
        DataStream<RowData> rowDataStream = airlineDataStream.map((AirlineJsonData airlineJsonData) -> {
            GenericRowData rowData = new GenericRowData(RowKind.INSERT, fieldCount);
            rowData.setField(0, StringData.fromString(airlineJsonData.getEmailAddress()));
            rowData.setField(1, StringData.fromString(airlineJsonData.getDepartureTime()));
            rowData.setField(2, StringData.fromString(airlineJsonData.getDepartureAirportCode()));
            rowData.setField(3, StringData.fromString(airlineJsonData.getArrivalTime()));
            rowData.setField(4, StringData.fromString(airlineJsonData.getArrivalAirportCode()));
            rowData.setField(5, airlineJsonData.getFlightDuration());
            rowData.setField(6, StringData.fromString(airlineJsonData.getFlightNumber()));
            rowData.setField(7, StringData.fromString(airlineJsonData.getConfirmationCode()));
            rowData.setField(8, DecimalData.fromBigDecimal(airlineJsonData.getTicketPrice(), 10, 2));
            rowData.setField(9, StringData.fromString(airlineJsonData.getAircraft()));
            rowData.setField(10, StringData.fromString(airlineJsonData.getBookingAgencyEmail()));
            return rowData;
        });
        
        TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);

        // Create the table if it does not exist
        try {
            if (!catalog.tableExists(ObjectPath.fromString(databaseName + "." + tableName))) {
                tblEnv.executeSql(
                            "CREATE TABLE " + databaseName + "." + tableName + " ("
                                + "email_address STRING, "
                                + "departure_time STRING, "
                                + "departure_airport_code STRING, "
                                + "arrival_time STRING, "
                                + "arrival_airport_code STRING, "
                                + "flight_duration BIGINT,"
                                + "flight_number STRING, "
                                + "confirmation_code STRING, "
                                + "ticket_price DECIMAL(10,2), "
                                + "aircraft STRING, "
                                + "booking_agency_email STRING) "
                                + "PARTITIONED BY (arrival_airport_code) "
                                + "WITH ("
                                    + "'write.format.default' = 'parquet',"
                                    + "'write.target-file-size-bytes' = '134217728',"
                                    + "'write.delete.mode' = 'merge-on-read',"
                                    + "'write.update.mode' = 'merge-on-read',"
                                    + "'format-version' = '2'"
                                + ");"
                        );
            } else {
                LOGGER.debug("Table {}.{} already exists.", databaseName, tableName);
            }
        } catch (CatalogException e) {
            LOGGER.error("A critical error occurred during the processing of the table: ", e);
            throw new RuntimeException("A critical error occurred during the processing of the table", e);
        }

        /*
         * Serializable loader to load an Apache Iceberg Table.  Apache Flink needs to get Table objects in the cluster,
         * not just on the client side. So we need an Iceberg table loader to get the Table object.
         */
        TableLoader tableLoader = TableLoader.fromCatalog(catalogLoader, tableIdentifier);

        /*
         * Writes data from the Apache Flink datastream to an Apache Iceberg table using upsert logic, where updates or insertions 
         * are decided based on the specified equality fields (i.e., "email_address", "departure_airport_code", "arrival_airport_code").
         */
        FlinkSink
            .forRowData(rowDataStream)
            .tableLoader(tableLoader)
            .upsert(true)
            .equalityFieldColumns(Arrays.asList("email_address", "departure_airport_code", "arrival_airport_code"))
            .append()
            .name(tableName + "_iceberg_sink");
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}