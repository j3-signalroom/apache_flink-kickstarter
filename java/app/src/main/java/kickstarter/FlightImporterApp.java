/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class imports flight data from `airline.sunset` and `airline.skyone` Kafka topics
 * and converts it to a unified format for the `airline.flight` Kafka topic.
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
package kickstarter;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.*;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import java.util.*;
import java.time.*;
import java.time.format.*;
import org.slf4j.*;

import kickstarter.model.*;


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
        /*
         * Retrieve the value(s) from the command line argument(s)
         */
        String serviceAccountUser = Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER);

        // --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG)
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        /*
		 * --- Kafka Consumer Config
		 * Retrieve the properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
		 * Then ingest properties into the Flink app
		 */
        DataStream<Properties> dataStreamConsumerProperties = 
			env.fromData(new Properties())
			   .map(new KafkaClientPropertiesLookup(true, serviceAccountUser))
			   .name("kafka_consumer_properties");
		Properties consumerProperties = new Properties();

        /*
		 * Execute the data stream and collect the properties.
		 * 
		 * Note, the try-with-resources block ensures that the close() method of the CloseableIterator is
		 * called automatically at the end, even if an exception occurs during iteration.
		 */
		try {
		    dataStreamConsumerProperties
                .executeAndCollect()
                .forEachRemaining(typeValue -> {
                    consumerProperties.putAll(typeValue);
                });
        } catch (final Exception e) {
            System.out.println("The Flink App stopped during the reading of the custom data source stream because of the following: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
		}

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
        try{
            dataStreamProducerProperties.executeAndCollect()
                                        .forEachRemaining(typeValue -> {
                                            producerProperties.putAll(typeValue);
                                        });
        } catch (final Exception e) {
            System.out.println("The Flink App stopped during the reading of the custom data source stream because of the following: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `airline.skyone`
         */
        @SuppressWarnings("unchecked")
        KafkaSource<AirlineData> skyOneSource = KafkaSource.<AirlineData>builder()
            .setProperties(consumerProperties)
            .setTopics("airline.skyone")
            .setGroupId("skyone_group")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema(AirlineData.class))
            .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStream<AirlineData> skyOneStream = env
            .fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `airline.sunset`
         */
		@SuppressWarnings("unchecked")
        KafkaSource<AirlineData> sunsetSource = KafkaSource.<AirlineData>builder()
            .setProperties(consumerProperties)
            .setTopics("airline.sunset")
            .setGroupId("sunset_group")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema(AirlineData.class))
            .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStream<AirlineData> sunsetStream = env
            .fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.flight` with the
         * specified serializer
         */
		KafkaRecordSerializationSchema<FlightData> flightSerializer = KafkaRecordSerializationSchema.<FlightData>builder()
            .setTopic("airline.flight")
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
	public static DataStream<FlightData> defineWorkflow(DataStream<AirlineData> skyOneSource, DataStream<AirlineData> sunsetSource) {
        DataStream<FlightData> skyOneFlightStream = 
            skyOneSource
                .filter(flight -> LocalDateTime.parse(flight.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).isAfter(LocalDateTime.now()))
                .map(AirlineData::toFlightData);

		DataStream<FlightData> sunsetFlightStream = 
            sunsetSource
            .filter(flight -> LocalDateTime.parse(flight.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).isAfter(LocalDateTime.now()))
                .map(AirlineData::toFlightData);

		return skyOneFlightStream.union(sunsetFlightStream);
    }
}