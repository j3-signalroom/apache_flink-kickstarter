/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class imports flight data from `airline.sunset` and `airline.skyone` Kafka topics
 * and converts it to a unified format for the `airline.all` Kafka topic.
 * 
 * ------------------------------------------------------------------------------------------
 * I had a question, can you combine the Flink DataStream API and Table API in the same DAG?
 * 
 * The answer is yes, you can combine the Flink DataStream API and Table API in the same 
 * Directed Acyclic Graph (DAG) to leverage the strengths of both APIs within a single Flink
 * application.  This is particularly useful when you want to perform some complex event 
 * processing that is more naturally expressed in the DataStream API, and then switch to the 
 * Table API for more declarative and SQL-like processing, or vice versa.  This where I bring
 * Apache Iceberg into the mix.  Apache Iceberg is a table format that is designed to be used
 * with the Flink Table API.  In this DAG, I use Apache Iceberg to store the unified flight
 * data in the `db_example.airline_flight_data` table.
 */
package apache_flink.kickstarter;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.*;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import java.util.*;
import java.time.*;
import org.slf4j.*;

import apache_flink.kickstarter.model.*;


public class FlightImporterApp {
    private static final Logger logger = LoggerFactory.getLogger(FlightImporterApp.class);


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
    @SuppressWarnings("rawtypes")
    public static void main(String[] args) throws Exception {
        // --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG)
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // --- Set up the table environment to work with the Table and SQL API in Flink using Java
        final StreamTableEnvironment tableEnv = 
            StreamTableEnvironment.create(env, EnvironmentSettings.newInstance().inStreamingMode().build());

        /*
         * --- Create an Apache Iceberg Catalog using Project Nessie
         * 
         * Project Nessie brings this Git-like workflow and benefits to your data stack.  Nessie is a 
         * transactional metastore that tracks the state and changes of all tables in the catalog 
         * through metadata via commits and alternate isolated branches like Git.
         */
        tableEnv.executeSql(
                "CREATE CATALOG iceberg WITH ("
                        + "'type'='iceberg',"
                        + "'catalog-impl'='org.apache.iceberg.nessie.NessieCatalog',"
                        + "'uri'='http://localhost:19120/api/v1',"
                        + "'ref'='main',"
                        + "'warehouse' = '/opt/flink/data/warehouse'"
                        + ")");

        // --- List all avialable Apache Iceberg Catalogs
        TableResult result = tableEnv.executeSql("SHOW CATALOGS");
        result.print();
/*
        // --- Set the current Apache Iceberg Catalog to the new Catalog
        tableEnv.useCatalog("iceberg");

        // --- Create a database in the current Catalog
        tableEnv.executeSql("CREATE DATABASE IF NOT EXISTS db_example");

        // --- Create the Apache Iceberg Table
        tableEnv.executeSql(
                "CREATE TABLE IF NOT EXISTS db_example.airline_flight_data ("
                        + "email_address STRING,"
                        + "departure_time STRING,"
                        + "departure_airport_code STRING,"
                        + "arrival_time STRING,"
                        + "arrival_airport_code STRING,"
                        + "flight_number STRING,"
                        + "confirmation_code STRING"
                        + ")");
*/
        /*
		 * --- Kafka Consumer Config
		 * Retrieve the properties from the local properties files, or from AWS
         * Secrets Manager and AWS Systems Manager Parameter Store.  Then ingest
		 * properties into the Flink app
		 */
        DataStream<Properties> dataStreamConsumerProperties = 
			env.fromData(new Properties())
			   .map(new KafkaClientPropertiesLookup(true, Common.getAppOptions(args)))
			   .name("kafka_consumer_properties");
		Properties consumerProperties = new Properties();
		dataStreamConsumerProperties.executeAndCollect()
                                    .forEachRemaining(typeValue -> {
                                        consumerProperties.putAll(typeValue);
                                    });

        /*
		 * --- Kafka Producer Config
		 * Retrieve the properties from the local properties files, or from AWS
         * Secrets Manager and AWS Systems Manager Parameter Store.  Then ingest
		 * properties into the Flink app
		 */
        DataStream<Properties> dataStreamProducerProperties = 
			env.fromData(new Properties())
			   .map(new KafkaClientPropertiesLookup(false, Common.getAppOptions(args)))
			   .name("kafka_producer_properties");
		Properties producerProperties = new Properties();
		dataStreamProducerProperties.executeAndCollect()
                                    .forEachRemaining(typeValue -> {
                                        producerProperties.putAll(typeValue);
                                    });

        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `airline.skyone`
         */
        @SuppressWarnings("unchecked")
        KafkaSource<SkyOneAirlinesFlightData> skyOneSource = KafkaSource.<SkyOneAirlinesFlightData>builder()
            .setProperties(consumerProperties)
            .setTopics("airline.skyone")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema(SkyOneAirlinesFlightData.class))
            .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStream<SkyOneAirlinesFlightData> skyOneStream = env
            .fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `airline.sunset`
         */
		@SuppressWarnings("unchecked")
        KafkaSource<SunsetAirFlightData> sunsetSource = KafkaSource.<SunsetAirFlightData>builder()
            .setProperties(consumerProperties)
            .setTopics("airline.sunset")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema(SunsetAirFlightData.class))
            .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStream<SunsetAirFlightData> sunsetStream = env
            .fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.all` with the
         * specified serializer
         */
		KafkaRecordSerializationSchema<FlightData> flightSerializer = KafkaRecordSerializationSchema.<FlightData>builder()
            .setTopic("airline.all")
			.setValueSerializationSchema(new JsonSerializationSchema<FlightData>(Common::getMapper))
            .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        KafkaSink<FlightData> flightSink = KafkaSink.<FlightData>builder()
            .setKafkaProducerConfig(producerProperties)
            .setRecordSerializer(flightSerializer)
            .build();

        /*
         * Defines the workflow for the Flink job graph (DAG) by connecting the data streams and
         * applying transformations to the data streams
         */
        defineWorkflow(skyOneStream, sunsetStream)
            .sinkTo(flightSink)
            .name("flightdata_sink");

        try {
            // --- Execute the Flink job graph (DAG)
            env.execute("FlightImporterApp");
        } catch (Exception e) {
            logger.error("The App stopped early due to the following: {}", e.getMessage());
        }
    }

    /**
     * This method defines the workflow for the Flink job graph (DAG) by connecting the 
     * data streams and applying transformations to the data streams.
     * 
     * @param skyOneSource - The data stream source for the `airline.skyone` Kafka topic
     * @param sunsetSource - The data stream source for the `airline.sunset` Kafka topic
     * @return The data stream that is the result of the transformations
     */
	public static DataStream<FlightData> defineWorkflow(DataStream<SkyOneAirlinesFlightData> skyOneSource, DataStream<SunsetAirFlightData> sunsetSource) {
        DataStream<FlightData> skyOneFlightStream =  skyOneSource
			.filter(flight -> flight.getFlightArrivalTime().isAfter(ZonedDateTime.now()))
			.map(SkyOneAirlinesFlightData::toFlightData);

		DataStream<FlightData> sunsetFlightStream = sunsetSource
            .filter(flight -> flight.getArrivalTime().isAfter(ZonedDateTime.now()))
            .map(flight -> flight.toFlightData());

		return skyOneFlightStream.union(sunsetFlightStream);
    }
}