/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
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
import org.apache.flink.table.catalog.CatalogDatabaseImpl;
import org.apache.iceberg.flink.FlinkCatalog;
import java.util.*;
import java.util.stream.StreamSupport;

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
			   .map(new KafkaClientPropertiesLookup(false, Common.getAppOptions(args)))
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

        // --- Define the schema for the in-memory table
        Schema schema = 
            Schema.newBuilder()
                   .column("email_address", DataTypes.STRING())
                   .column("departure_time", DataTypes.STRING())
                   .column("departure_airport_code", DataTypes.STRING())
                   .column("arrival_time", DataTypes.STRING())
                   .column("arrival_airport_code", DataTypes.STRING())
                   //.column("flight_duration", DataTypes.BIGINT())
                   .column("flight_number", DataTypes.STRING())
                   .column("confirmation_code", DataTypes.STRING())
                   //.column("ticket_price", DataTypes.DECIMAL(10, 2))
                   .column("aircraft", DataTypes.STRING())
                   .column("booking_agency_email", DataTypes.STRING())
                   .build();

        /*
         * Sets up a Flink POJO source to consume data
         */
        DataStream<AirlineData> skyOneStream = 
            env.fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

        // --- Convert DataStream to Table
        Table skyOneTable = tblEnv.fromDataStream(skyOneStream, schema);
        tblEnv.createTemporaryView("SkyOneTable", skyOneTable);

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


        // --- Convert DataStream to Table
        Table sunsetTable = tblEnv.fromDataStream(sunsetStream, schema);
        tblEnv.createTemporaryView("SunsetTable", sunsetTable);

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

        /*
         * Define the CREATE CATALOG Flink SQL statement to register the Iceberg catalog
         * using the HadoopCatalog to store metadata in AWS S3 (i.e., s3a://), a Hadoop- 
         * compatible filesystem.  Then execute the Flink SQL statement to register the
         * Iceberg catalog 
         */
        String catalogName = "apache_kickstarter";
        String bucketName = Common.getAppOptions(args).replace("_", "-");  // --- To follow S3 bucket naming convention, replace underscores with hyphens if exist
        try {
            if(!Common.isCatalogExist(tblEnv, catalogName)) {
                /*
                 * Execute the CREATE CATALOG Flink SQL statement to register the Iceberg catalog.
                 */
                tblEnv.executeSql(
                    "CREATE CATALOG " + catalogName + " WITH (" 
                        + "'type' = 'iceberg',"
                        + "'catalog-type' = 'hadoop',"
                        + "'warehouse' = 's3a://" + bucketName + "/warehouse',"
                        + "'property-version' = '1',"
                        + "'io-impl' = 'org.apache.iceberg.hadoop.HadoopFileIO'"
                        + ");"
                );
            } else {
                System.out.println("The " + catalogName + " catalog already exists.");
            }
        } catch(final Exception e) {
            System.out.println("A critical error occurred to during the processing of the catalog because " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // --- Use the Iceberg catalog
        tblEnv.useCatalog(catalogName);

        // --- Print the current catalog name
        System.out.println("Current catalog: " + tblEnv.getCurrentCatalog());

        // --- Check if the database exists.  If not, create it
        String databaseName = "airlines";

        // Check if the namespace exists, if not, create it
        try {
            org.apache.flink.table.catalog.Catalog catalog = tblEnv.getCatalog("apache_kickstarter").orElseThrow(() -> new RuntimeException("Catalog not found"));
            if (catalog instanceof FlinkCatalog) {
                FlinkCatalog flinkCatalog = (FlinkCatalog) catalog;
                if (!flinkCatalog.databaseExists(databaseName)) {
                    flinkCatalog.createDatabase(databaseName, new CatalogDatabaseImpl(new HashMap<>(), "The Airlines flight data database."), false);
                }
            }
            tblEnv.useDatabase(databaseName);
        } catch (Exception e) {
            System.out.println("A critical error occurred during the processing of the database because " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // --- Print the current database name
        System.out.println("Current database: " + tblEnv.getCurrentDatabase());

        // --- Set up the arrays for the table names and tables
        String tableNames[] = {"skyone_airline", "sunset_airline"};
        Table tables[] = {skyOneTable, sunsetTable};
        int tableIndex = -1;

        /*
         * Check if the table exists.  If not, create it.  Then insert the data into
         * the table(s).
         */
        for (String tableName : tableNames) {
            tableIndex++;

            // --- Create the table path
            String tablePath = databaseName + "." + tableName;

            try {
                TableResult result = tblEnv.executeSql("SHOW TABLES IN " + databaseName);
                @SuppressWarnings("null")
                boolean tableExists = StreamSupport.stream(Spliterators
                                                           .spliteratorUnknownSize(result.collect(), Spliterator.ORDERED), false)
                                                           .anyMatch(row -> row.getField(0).equals(tableName));
                if(!tableExists) {
                    tblEnv.executeSql(
                        "CREATE TABLE " + tablePath + " ("
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
                } else {
                    System.out.println("The " + tablePath + " table already exists.");
                }
            } catch(final Exception e) {
                System.out.println("A critical error occurred to during the processing of the table because " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }

            /*
             * Convert datastream into table and then insert data into physical table
             */
            tables[tableIndex].executeInsert(tablePath);
        }

        try {
            // --- Execute the Flink job graph (DAG)
            env.execute("DataGeneratorApp");
        } catch (Exception e) {
            System.out.println("The Flink App stopped early due to the following: " + e.getMessage());
            e.printStackTrace();
        }
	}
}