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

import kickstarter.model.*;


/**
 * This class creates fake flight data for fictional airlines <b>Sunset Air</b> and 
 * <b>Sky One</b> Airlines," and sends it to the Kafka topics `airline.sunset` and `airline.skyone`,
 * respectively.
 */
public class DataGeneratorApp {
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
        /*
         * Retrieve the value(s) from the command line argument(s)
         */
        String serviceAccountUser = Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER);

		// --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG)
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // --- Enable checkpointing every 5000 milliseconds (5 seconds)
        env.enableCheckpointing(5000);

        /*
         * Set timeout to 60 seconds
         * The maximum amount of time a checkpoint attempt can take before being discarded.
         */
        env.getCheckpointConfig().setCheckpointTimeout(60000);

        /*
         * Set the maximum number of concurrent checkpoints to 1 (i.e., only one checkpoint
         * is created at a time)
         */
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);

        // --- Create a TableEnvironment
        EnvironmentSettings settings = 
            EnvironmentSettings.newInstance()
                               .inStreamingMode()
                               .build();
        StreamTableEnvironment tblEnv = StreamTableEnvironment.create(env, settings);

		/*
		 * --- Kafka Producer Config
		 * Retrieve the properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
		 * Then ingest properties into the Flink app
		 */
        DataStream<Properties> dataStreamProducerProperties = 
			env.fromData(new Properties())
			   .map(new KafkaClientPropertiesLookup(false, serviceAccountUser))
			   .name("kafka_producer_properties");
		Properties producerProperties = new Properties();

		/*
		 * Execute the data stream and collect the properties.
		 * 
		 * Note, the try-with-resources block ensures that the close() method of the CloseableIterator is
		 * called automatically at the end, even if an exception occurs during iteration.
		 */
		try {
			dataStreamProducerProperties
				.executeAndCollect()
                .forEachRemaining(typeValue -> {
                    producerProperties.putAll(typeValue);
                });
		} catch (final Exception e) {
            System.out.println("The Flink App stopped during the reading of the custom data source stream because of the following: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
		}

        /*
         * Create a data generator source
         */
        DataGeneratorSource<AirlineData> skyOneSource =
            new DataGeneratorSource<>(
                index -> DataGenerator.generateAirlineFlightData("SKY1"),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(1),
                Types.POJO(AirlineData.class)
            );

        /*
         * Sets up a Flink POJO source to consume data
         */
        DataStream<AirlineData> skyOneStream = 
            env.fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.skyone` with the
         * specified serializer
         */
        KafkaRecordSerializationSchema<AirlineData> skyOneSerializer = 
            KafkaRecordSerializationSchema.<AirlineData>builder()
                .setTopic("airline.skyone")
                .setValueSerializationSchema(new JsonSerializationSchema<>(Common::getMapper))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        KafkaSink<AirlineData> skyOneSink = 
            KafkaSink.<AirlineData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(skyOneSerializer)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        /*
         * Adds the given Sink to the DAG. Note only streams with sinks added will be executed
         * once the StreamExecutionEnvironment.execute() method is called
         */
        skyOneStream.sinkTo(skyOneSink).name("skyone_sink");

        /*
         * Sets up a Flink POJO source to consume data
         */
        DataGeneratorSource<AirlineData> sunsetSource =
            new DataGeneratorSource<>(
                index -> DataGenerator.generateAirlineFlightData("SUN"),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(1),
                Types.POJO(AirlineData.class)
            );

        DataStream<AirlineData> sunsetStream = 
            env.fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.sunset` with the
         * specified serializer
         */
        KafkaRecordSerializationSchema<AirlineData> sunsetSerializer = 
            KafkaRecordSerializationSchema.<AirlineData>builder()
                .setTopic("airline.sunset")
                .setValueSerializationSchema(new JsonSerializationSchema<>(Common::getMapper))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        KafkaSink<AirlineData> sunsetSink = 
            KafkaSink.<AirlineData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(sunsetSerializer)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        /*
         * Adds the given Sink to the DAG. Note only streams with sinks added will be executed
         * once the StreamExecutionEnvironment.execute() method is called
         */
        sunsetStream.sinkTo(sunsetSink).name("sunset_sink");

        String catalogName = "apache_kickstarter";
        String bucketName = serviceAccountUser.replace("_", "-");  // --- To follow S3 bucket naming convention, replace underscores with hyphens if exist
        String warehousePath = "s3://" + bucketName + "/warehouse";
        String catalogImpl = "org.apache.iceberg.aws.glue.GlueCatalog";
        String ioImpl = "org.apache.iceberg.aws.s3.S3FileIO";
        String databaseName = "airlines";

        // --- Configure the AWS Glue Catalog Properties
        Map<String, String> catalogProperties = new HashMap<>();
        catalogProperties.put("type", "iceberg");
        catalogProperties.put("warehouse", warehousePath);
        catalogProperties.put("catalog-impl", catalogImpl);
        catalogProperties.put("io-impl", ioImpl);
        catalogProperties.put("glue.skip-archive", "true");
        catalogProperties.put("glue.region", "us-east-1");
        
        // --- Use the CatalogLoader since an external metastore is used (AWS Glue Catalog)
        CatalogLoader catalogLoader = CatalogLoader.custom(catalogName, catalogProperties,  new Configuration(false), catalogImpl);

        // --- Describes and configures the catalog for the Table API and SQL
        CatalogDescriptor catalogDescriptor = CatalogDescriptor.of(catalogName, org.apache.flink.configuration.Configuration.fromMap(catalogProperties));

        // --- Create the catalog, use it, and get the instantiated catalog
        tblEnv.createCatalog(catalogName, catalogDescriptor);
        tblEnv.useCatalog(catalogName);
        org.apache.flink.table.catalog.Catalog catalog = tblEnv.getCatalog("apache_kickstarter").orElseThrow(() -> new RuntimeException("Catalog not found"));

        // --- Print the current catalog name
        System.out.println("Current catalog: " + tblEnv.getCurrentCatalog());

        // --- Check if the database exists.  If not, create it
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

        // --- Print the current database name
        System.out.println("Current database: " + tblEnv.getCurrentDatabase());

        // --- Define the RowType for the RowData
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
        
        // --- Convert DataStream<AirlineData> to DataStream<RowData>
        DataStream<RowData> skyOneRowData = skyOneStream.map(new MapFunction<AirlineData, RowData>() {
            @Override
            public RowData map(AirlineData airlineData) throws Exception {
                GenericRowData rowData = new GenericRowData(RowKind.INSERT, rowType.getFieldCount());
                rowData.setField(0, airlineData.getEmailAddress());
                rowData.setField(1, airlineData.getDepartureTime());
                rowData.setField(2, airlineData.getDepartureAirportCode());
                rowData.setField(3, airlineData.getArrivalTime());
                rowData.setField(4, airlineData.getArrivalAirportCode());
                rowData.setField(5, airlineData.getFlightDuration());
                rowData.setField(6, airlineData.getFlightNumber());
                rowData.setField(7, airlineData.getConfirmationCode());
                rowData.setField(8, airlineData.getTicketPrice());
                rowData.setField(9, airlineData.getAircraft());
                rowData.setField(10, airlineData.getBookingAgencyEmail());
                return rowData;
            }
        });
        
        TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, "skyone_airline");

        // Create the table if it does not exist
        if (!catalog.tableExists(ObjectPath.fromString(databaseName + "." + "skyone_airline"))) {
            tblEnv.executeSql(
                        "CREATE TABLE " + databaseName + "." + "skyone_airline" + " ("
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

        // ---
        TableLoader tableLoaderSkyOne = TableLoader.fromCatalog(catalogLoader, tableIdentifier);

        FlinkSink
            .forRowData(skyOneRowData)
            .tableLoader(tableLoaderSkyOne)
            .upsert(true)
            .equalityFieldColumns(Arrays.asList("email_address", "departure_airport_code", "arrival_airport_code"))
            .append();

        // Create the table if it does not exist
        if (!catalog.tableExists(ObjectPath.fromString(databaseName + "." + "sunset_airline"))) {
            tblEnv.executeSql(
                        "CREATE TABLE " + databaseName + "." + "sunset_airline" + " ("
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

        // --- Convert DataStream<AirlineData> to DataStream<RowData>
        DataStream<RowData> sunsetRowData = sunsetStream.map(new MapFunction<AirlineData, RowData>() {
            @Override
            public RowData map(AirlineData airlineData) throws Exception {
                GenericRowData rowData = new GenericRowData(RowKind.INSERT, rowType.getFieldCount());
                rowData.setField(0, airlineData.getEmailAddress());
                rowData.setField(1, airlineData.getDepartureTime());
                rowData.setField(2, airlineData.getDepartureAirportCode());
                rowData.setField(3, airlineData.getArrivalTime());
                rowData.setField(4, airlineData.getArrivalAirportCode());
                rowData.setField(5, airlineData.getFlightDuration());
                rowData.setField(6, airlineData.getFlightNumber());
                rowData.setField(7, airlineData.getConfirmationCode());
                rowData.setField(8, airlineData.getTicketPrice());
                rowData.setField(9, airlineData.getAircraft());
                rowData.setField(10, airlineData.getBookingAgencyEmail());
                return rowData;
            }
        });
        
        // ---
        TableLoader tableLoaderSunset = TableLoader.fromCatalog(catalogLoader, TableIdentifier.of(databaseName, "sunset_airline"));

        // --- Initialize a FlinkSink.Builder to export the data from input data stream with RowDatas into iceberg table
        FlinkSink
            .forRowData(sunsetRowData)
            .tableLoader(tableLoaderSunset)
            .upsert(true)
            .equalityFieldColumns(Arrays.asList("email_address", "departure_airport_code", "arrival_airport_code"))
            .append();

        try {
            // --- Execute the Flink job graph (DAG)
            env.execute("DataGeneratorApp");
        } catch (Exception e) {
            System.out.println("The Flink App stopped early due to the following: " + e.getMessage());
            e.printStackTrace();
        }
	}
}