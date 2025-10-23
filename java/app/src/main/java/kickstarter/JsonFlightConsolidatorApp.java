/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class imports flight data from `sunset` and `skyone` Kafka topics
 * and converts it to a unified format for the `flight` Kafka topic.
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import kickstarter.helper.SnakeCaseJsonDeserializationSchema;
import kickstarter.model.AirlineJsonData;
import kickstarter.model.FlightJsonData;


public class JsonFlightConsolidatorApp {
    private static final Logger logger = Logger.getLogger(JsonFlightConsolidatorApp.class.getName());
    

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
        
        // --- Kafka Consumer and Producer Client Properties
        Properties consumerProperties = Common.collectConfluentProperties(env, serviceAccountUser, true);
        Properties producerProperties = Common.collectConfluentProperties(env, serviceAccountUser, false);
        
        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `skyone`
         */
        KafkaSource<AirlineJsonData> skyOneSource = KafkaSource.<AirlineJsonData>builder()
            .setProperties(consumerProperties)
            .setTopics("skyone")
            .setGroupId("skyone_group")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer((new SnakeCaseJsonDeserializationSchema<>(AirlineJsonData.class)))
            .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStream<AirlineJsonData> skyOneStream = env
            .fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `sunset`
         */
		@SuppressWarnings("unchecked")
        KafkaSource<AirlineJsonData> sunsetSource = KafkaSource.<AirlineJsonData>builder()
            .setProperties(consumerProperties)
            .setTopics("sunset")
            .setGroupId("sunset_group")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SnakeCaseJsonDeserializationSchema(AirlineJsonData.class))
            .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStream<AirlineJsonData> sunsetStream = env
            .fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `flight` with the
         * specified serializer
         */
		KafkaRecordSerializationSchema<FlightJsonData> flightSerializer = KafkaRecordSerializationSchema.<FlightJsonData>builder()
            .setTopic("flight")
			.setValueSerializationSchema(new JsonSerializationSchema<>(Common::getMapper))
            .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        KafkaSink<FlightJsonData> flightSink = KafkaSink.<FlightJsonData>builder()
            .setKafkaProducerConfig(producerProperties)
            .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
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
            env.execute("JsonFlightConsolidatorApp");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "The App stopped early due to the following: ", e.getMessage());
        }
    }

    /**
     * This method defines the workflow for the Flink job graph (DAG) by connecting the 
     * data streams and applying transformations to the data streams.
     * 
     * @param skyOneSource - The data stream source for the `skyone` Kafka topic
     * @param sunsetSource - The data stream source for the `sunset` Kafka topic
     * @return The data stream that is the result of the transformations
     */
	public static DataStream<FlightJsonData> defineWorkflow(DataStream<AirlineJsonData> skyOneSource, DataStream<AirlineJsonData> sunsetSource) {
        DataStream<FlightJsonData> skyOneFlightStream = 
            skyOneSource
                .filter(flight -> LocalDateTime.parse(flight.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).isAfter(LocalDateTime.now()))
                .map(flight -> flight.toFlightData("SkyOne"));

		DataStream<FlightJsonData> sunsetFlightStream = 
            sunsetSource
            .filter(flight -> LocalDateTime.parse(flight.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).isAfter(LocalDateTime.now()))
                .map(flight -> flight.toFlightData("Sunset"));

		return skyOneFlightStream.union(sunsetFlightStream);
    }
}