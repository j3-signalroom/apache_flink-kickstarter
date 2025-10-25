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

import org.apache.avro.Conversions.DecimalConversion;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroSerializationSchema;
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
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.sink.FlinkSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kickstarter.model.AirlineAvroData;


/**
 * This class creates fake flight data for two fictional airlines <b>Sunset Air</b> and 
 * <b>Sky One</b> Airlines</b>.  Then sinks the data to Kafka Topics and Apache Iceberg
 * Tables, respectively.
 */
public class AvroDataGeneratorApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(AvroDataGeneratorApp.class);
    private static final DecimalConversion DECIMAL_CONVERSION = new DecimalConversion();
    private static final LogicalTypes.Decimal DECIMAL_TYPE = LogicalTypes.decimal(10, 2);
    private static final Schema DECIMAL_SCHEMA = DECIMAL_TYPE.addToSchema(Schema.create(Schema.Type.BYTES));

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
        String serviceAccountUser = Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER);
        String awsRegion = System.getenv().getOrDefault("AWS_REGION", Common.getAppArgumentValue(args, Common.ARG_AWS_REGION));
        String bucketName = System.getenv("AWS_S3_BUCKET");

        // --- Validate required configurations
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

        LOGGER.info("Configuration loaded - AWS Region: {}, S3 Bucket: {}", awsRegion, bucketName);

        // --- Create a configuration to force Avro serialization instead of Kyro serialization.
        org.apache.flink.configuration.Configuration config = new org.apache.flink.configuration.Configuration();
        config.set(PipelineOptions.FORCE_AVRO, true);

        // --- Configure parent-first classloading for metrics
        config.setString("classloader.parent-first-patterns.additional", "com.codahale.metrics;io.dropwizard.metrics");

		// --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG).
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(config);

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

        // --- Set the parallelism for the entire job.
        env.setParallelism(6);

        // --- Create a StreamTableEnvironment.
        EnvironmentSettings settings = 
            EnvironmentSettings.newInstance()
                               .inStreamingMode()
                               .build();
        StreamTableEnvironment tblEnv = StreamTableEnvironment.create(env, settings);

		// --- Kafka Producer Client Properties.
        Properties producerProperties = Common.collectConfluentProperties(env, serviceAccountUser, false);

        // --- Retrieve the schema registry properties and store it in a map.
        Map<String, String> registryConfigs = Common.extractRegistryConfigs(producerProperties);

        // --- Create the data streams for the two airlines.
        DataStream<AirlineAvroData> skyOneDataStream = SinkToKafkaTopic(env, "SKY1", "skyone", producerProperties, registryConfigs);
        DataStream<AirlineAvroData> sunsetDataStream = SinkToKafkaTopic(env, "SUN", "sunset", producerProperties, registryConfigs);

        // --- Describes and configures the catalog for the Table API and Flink SQL.
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

        // --- Print the current catalog name.
        System.out.println("Current catalog: " + tblEnv.getCurrentCatalog());

        // --- Check if the database exists.  If not, create it.
        try {
            if (!catalog.databaseExists(databaseName)) {
                catalog.createDatabase(databaseName, new CatalogDatabaseImpl(new HashMap<>(), "The Airlines flight data database."), false);
            }
            tblEnv.useDatabase(databaseName);
        } catch (CatalogException | DatabaseAlreadyExistException e) {
            LOGGER.error("A critical error occurred during the processing of the database because {}", e.getMessage());
            System.exit(1);
        }

        // --- Print the current database name.
        System.out.println("Current database: " + tblEnv.getCurrentDatabase());

        // --- Define the RowType for the RowData.
        RowType rowType = RowType.of(
            new org.apache.flink.table.types.logical.LogicalType[] {
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
        SinkToIcebergTable(tblEnv, catalog, catalogLoader, databaseName, rowType.getFieldCount(), "skyone_airline", skyOneDataStream);
        SinkToIcebergTable(tblEnv, catalog, catalogLoader, databaseName, rowType.getFieldCount(), "sunset_airline", sunsetDataStream);

        // --- Execute the Flink job graph (DAG)
        try {            
            env.execute("AvroDataGeneratorApp");
        } catch (Exception e) {
            LOGGER.error("The App stopped early due to the following: {}", e.getMessage(), e);
            throw e; // --- Rethrow the exception to signal failure.
        }
	}

    /**
     * This method is used to sink the data from the input data stream into the Kafka topic.
     * 
     * @param env The StreamExecutionEnvironment.
     * @param airlinePrefix The airline prefix.
     * @param airline The airline name.
     * @param producerProperties The Kafka producer properties.
     * @param registryConfigs The schema registry properties.
     * 
     * @return The data stream.
     */
    private static DataStream<AirlineAvroData> SinkToKafkaTopic(final StreamExecutionEnvironment env, final String airlinePrefix, final String airline, Properties producerProperties, Map<String, String> registryConfigs) {
        // --- Create a data generator source.
        DataGeneratorSource<AirlineAvroData> airlineSource =
            new DataGeneratorSource<>(
                index -> DataGenerator.generateAirlineAvroData(airlinePrefix),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(1),
                Types.POJO(AirlineAvroData.class)
            );

        // --- Sets up a Flink Avro Generic Record source to consume data.
        DataStream<AirlineAvroData> airlineDataStream = env.fromSource(airlineSource, WatermarkStrategy.noWatermarks(), airline + "_source");

        // --- Sets up a Flink Kafka sink to produce data to the Kafka topic with the specified serializer.
        final String topicName = airline + "_avro";
        KafkaRecordSerializationSchema<AirlineAvroData> airlineSerializer = 
            KafkaRecordSerializationSchema.<AirlineAvroData>builder()
                .setTopic(topicName)
                .setValueSerializationSchema(ConfluentRegistryAvroSerializationSchema.forSpecific(AirlineAvroData.class, topicName + "-value", producerProperties.getProperty("schema.registry.url"), registryConfigs))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG).
         */
        KafkaSink<AirlineAvroData> airlineSink = 
            KafkaSink.<AirlineAvroData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(airlineSerializer)
                .setTransactionalIdPrefix("avro-data-" + airline.toLowerCase() + "-") // apply unique prefix to prevent backchannel conflicts and potential memory leaks
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .build();

        /*
         * Adds the given Sink to the DAG. Note only streams with sinks added will be executed
         * once the StreamExecutionEnvironment.execute() method is called.
         */
        airlineDataStream.sinkTo(airlineSink).name(airline + "_sink");

        return airlineDataStream;
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
    private static void SinkToIcebergTable(final StreamTableEnvironment tblEnv, final org.apache.flink.table.catalog.Catalog catalog, final CatalogLoader catalogLoader, final String databaseName, final int fieldCount, final String tableName, DataStream<AirlineAvroData> airlineDataStream) {
        // --- Convert DataStream<AirlineData> to DataStream<RowData>
        @SuppressWarnings("Convert2Lambda")
        DataStream<RowData> rowDataStream = airlineDataStream.map(new MapFunction<AirlineAvroData, RowData>() {
            @Override
            public RowData map(AirlineAvroData airlineData) throws Exception {
                GenericRowData rowData = new GenericRowData(RowKind.INSERT, fieldCount);
                rowData.setField(0, StringData.fromString((String) airlineData.getEmailAddress()));
                rowData.setField(1, StringData.fromString((String) airlineData.getDepartureTime()));
                rowData.setField(2, StringData.fromString((String) airlineData.getDepartureAirportCode()));
                rowData.setField(3, StringData.fromString((String) airlineData.getArrivalTime()));
                rowData.setField(4, StringData.fromString((String) airlineData.getArrivalAirportCode()));
                rowData.setField(5, airlineData.getFlightDuration());
                rowData.setField(6, StringData.fromString((String) airlineData.getFlightNumber()));
                rowData.setField(7, StringData.fromString((String) airlineData.getConfirmationCode()));
                rowData.setField(8, DecimalData.fromBigDecimal(DECIMAL_CONVERSION.fromBytes(airlineData.getTicketPrice(), DECIMAL_SCHEMA, DECIMAL_TYPE), 10, 2));
                rowData.setField(9, StringData.fromString((String) airlineData.getAircraft()));
                rowData.setField(10, StringData.fromString((String) airlineData.getBookingAgencyEmail()));
                return rowData;
            }
        });
        
        TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);

        // --- Create the table if it does not exist.
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
                                + "'format-version' = '2');"
                    );
        }

        /*
         * Serializable loader to load an Apache Iceberg Table.  Apache Flink needs to get Table objects in the cluster,
         * not just on the client side. So we need an Iceberg table loader to get the Table object.
         */
        TableLoader tableLoader = TableLoader.fromCatalog(catalogLoader, tableIdentifier);

        /*
         * Writes data from the Apache Flink datastream to an Apache Iceberg table using upsert logic, where updates or insertions 
         * are decided based on the specified equality fields (i.e., "email_address", "departure_airport_code", "arrival_airport_code").
         * 
         * IMPORTANT: For Flink 2.x, the .append() method returns a DataStreamSink which must be used to properly attach
         * the sink to the execution graph. We chain .name() to set the operator name.
         */
        FlinkSink
            .forRowData(rowDataStream)
            .tableLoader(tableLoader)
            .upsert(true)
            .equalityFieldColumns(Arrays.asList("email_address", "departure_airport_code", "arrival_airport_code"))
            .append()
            .name(tableName + "_iceberg_sink");
    }
}
