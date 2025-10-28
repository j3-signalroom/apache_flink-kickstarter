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
import java.time.format.DateTimeParseException;
import java.util.Properties;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kickstarter.helper.SnakeCaseJsonDeserializationSchema;
import kickstarter.model.AirlineJsonData;
import kickstarter.model.FlightJsonData;


public class JsonFlightConsolidatorApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonFlightConsolidatorApp.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    

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
        String serviceAccountUser = System.getenv().getOrDefault("SERVICE_ACCOUNT_USER", Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER));

        // --- Validate required configuration
        if (serviceAccountUser == null || serviceAccountUser.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Service Account User is required. Set SERVICE_ACCOUNT_USER environment variable or use --service-account-user argument."
            );
        }

        LOGGER.info("Configuration loaded - Service Account User: {}", serviceAccountUser);

        // --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG)
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
        KafkaSource<AirlineJsonData> sunsetSource = KafkaSource.<AirlineJsonData>builder()
            .setProperties(consumerProperties)
            .setTopics("sunset")
            .setGroupId("sunset_group")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SnakeCaseJsonDeserializationSchema<>(AirlineJsonData.class))
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
        producerProperties.put("transaction.timeout.ms", "900000"); // 15m for long checkpoints
        producerProperties.put("enable.idempotence", "true");       // typically implied, explicit is fine
        KafkaSink<FlightJsonData> flightSink = KafkaSink.<FlightJsonData>builder()
            .setKafkaProducerConfig(producerProperties)
            .setTransactionalIdPrefix("json-flight-data-") // apply unique prefix to prevent backchannel conflicts and potential memory leaks
            .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
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
            LOGGER.error("The App stopped early due to the following: ", e);
            throw e;
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
                .filter(flight -> isValidFutureFlight(flight))
                .name("skyone_flight_filter").uid("skyone_flight_filter")
                .map(flight -> flight.toFlightData("SkyOne"))
                .name("skyone_flight_transform").uid("skyone_flight_transform");

		DataStream<FlightJsonData> sunsetFlightStream = 
            sunsetSource
                .filter(flight -> isValidFutureFlight(flight))
                .name("sunset_flight_filter").uid("sunset_flight_filter")
                .map(flight -> flight.toFlightData("Sunset"))
                .name("sunset_flight_transform").uid("sunset_flight_transform");

		return skyOneFlightStream.union(sunsetFlightStream);
    }

    /**
     * Validates that a flight has a valid arrival time in the future.
     * Includes null safety and exception handling to prevent job failures.
     * 
     * @param flight The flight data to validate
     * @return true if the flight has a valid future arrival time, false otherwise
     */
    private static boolean isValidFutureFlight(AirlineJsonData flight) {
        if (flight == null) {
            LOGGER.warn("Flight is null, filtering out");
            return false;
        }

        if (flight.getArrivalTime() == null) {
            LOGGER.warn("Flight has null arrival time, filtering out");
            return false;
        }

        try {
            LocalDateTime arrivalTime = LocalDateTime.parse(flight.getArrivalTime(), DATE_TIME_FORMATTER);
            return arrivalTime.isAfter(LocalDateTime.now());
        } catch (DateTimeParseException e) {
            LOGGER.warn("Failed to parse arrival time '{}': {}", flight.getArrivalTime(), e.getMessage());
            return false;
        }
    }
}