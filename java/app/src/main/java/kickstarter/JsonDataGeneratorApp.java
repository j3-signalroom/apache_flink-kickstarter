/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.*;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.data.*;
import org.apache.flink.table.types.logical.*;
import org.apache.flink.types.*;
import org.apache.flink.table.catalog.*;
import org.apache.iceberg.catalog.*;
import org.apache.iceberg.flink.*;
import org.apache.iceberg.flink.sink.FlinkSink;
import org.apache.hadoop.conf.Configuration;
import java.util.*;
import org.slf4j.*;

import kickstarter.model.*;


/**
 * This class creates fake flight data for two fictional airlines <b>Sunset Air</b> and 
 * <b>Sky One</b> Airlines</b>.  Then sinks the data to Kafka Topics and Apache Iceberg
 * Tables, respectively.
 */
public class JsonDataGeneratorApp {
    private static final Logger logger = LoggerFactory.getLogger(JsonDataGeneratorApp.class);


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
        String awsRegion = Common.getAppArgumentValue(args, Common.ARG_AWS_REGION);

		// --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG).
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        /*
         * Enable checkpointing every 5000 milliseconds (5 seconds).  Note, consider the
         * resource cost of checkpointing frequency, as short intervals can lead to higher
         * I/O and CPU overhead.  Proper tuning of checkpoint intervals depends on the
         * state size, latency requirements, and resource constraints.
         */
        env.enableCheckpointing(5000);

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
        KafkaSink<AirlineJsonData> skyOneSink = 
            KafkaSink.<AirlineJsonData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(skyOneSerializer)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
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
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        /*
         * Adds the given Sink to the DAG. Note only streams with sinks added will be executed
         * once the StreamExecutionEnvironment.execute() method is called.
         */
        sunsetStream.sinkTo(sunsetSink).name("sunset_sink");

        // --- Describes and configures the catalog for the Table API and SQL.
        String catalogName = "apache_kickstarter";
        String bucketName = serviceAccountUser.replace("_", "-");  // --- To follow S3 bucket naming convention, replace underscores with hyphens if exist in string.
        String catalogImpl = "org.apache.iceberg.aws.glue.GlueCatalog";
        String databaseName = "airlines";
        Map<String, String> catalogProperties = new HashMap<>();
        catalogProperties.put("type", "iceberg");
        catalogProperties.put("warehouse", "s3://" + bucketName + "/warehouse");
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
        } catch (Exception e) {
            System.out.println("A critical error occurred during the processing of the database because " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // --- Print the current database name.
        System.out.println("Current database: " + tblEnv.getCurrentDatabase());

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
        SinkToIcebergTable(tblEnv, catalog, catalogLoader, databaseName, rowType.getFieldCount(), "skyone_airline", skyOneStream);
        SinkToIcebergTable(tblEnv, catalog, catalogLoader, databaseName, rowType.getFieldCount(), "sunset_airline", sunsetStream);

        // --- Execute the Flink job graph (DAG)
        try {            
            env.execute("JsonDataGeneratorApp");
        } catch (Exception e) {
            logger.error("The App stopped early due to the following: {}", e.getMessage());
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
    private static void SinkToIcebergTable(final StreamTableEnvironment tblEnv, final org.apache.flink.table.catalog.Catalog catalog, final CatalogLoader catalogLoader, final String databaseName, final int fieldCount, final String tableName, DataStream<AirlineJsonData> airlineDataStream) {
        // --- Convert DataStream<AirlineJsonData> to DataStream<RowData>
        DataStream<RowData> skyOneRowData = airlineDataStream.map(new MapFunction<AirlineJsonData, RowData>() {
            @Override
            public RowData map(AirlineJsonData airlineJsonData) throws Exception {
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
            }
        });
        
        TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);

        // Create the table if it does not exist
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
                            + "WITH ("
                                + "'write.format.default' = 'parquet',"
                                + "'write.target-file-size-bytes' = '134217728',"
                                + "'partitioning' = 'arrival_airport_code',"
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
         */
        FlinkSink
            .forRowData(skyOneRowData)
            .tableLoader(tableLoader)
            .upsert(true)
            .equalityFieldColumns(Arrays.asList("email_address", "departure_airport_code", "arrival_airport_code"))
            .append();
    }
}