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
import java.util.Map;
import java.util.Properties;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kickstarter.model.AirlineAvroData;
import kickstarter.model.FlightAvroData;


public class AvroFlightConsolidatorApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(AvroFlightConsolidatorApp.class);

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
        String serviceAccountUser = Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER);

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
        
        // --- Retrieve the schema registry properties and store it in a map.
        Map<String, String> registryConfigs = Common.extractRegistryConfigs(producerProperties);
        
        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `skyone_avro`
         */
        KafkaSource<AirlineAvroData> skyOneSource = KafkaSource.<AirlineAvroData>builder()
            .setProperties(consumerProperties)
            .setTopics("skyone_avro")
            .setGroupId("skyone_group")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(ConfluentRegistryAvroDeserializationSchema.forSpecific(AirlineAvroData.class, producerProperties.getProperty("schema.registry.url"), registryConfigs))
            .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStream<AirlineAvroData> skyOneStream = env
            .fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `sunset_avro`
         */
        KafkaSource<AirlineAvroData> sunsetSource = KafkaSource.<AirlineAvroData>builder()
            .setProperties(consumerProperties)
            .setTopics("sunset_avro")
            .setGroupId("sunset_group")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(ConfluentRegistryAvroDeserializationSchema.forSpecific(AirlineAvroData.class, producerProperties.getProperty("schema.registry.url"), registryConfigs))
            .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStream<AirlineAvroData> sunsetStream = env
            .fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `flight_avro` with the
         * specified serializer
         */
        final String topicName = "flight_avro";
		KafkaRecordSerializationSchema<FlightAvroData> flightSerializer = KafkaRecordSerializationSchema.<FlightAvroData>builder()
            .setTopic(topicName)
            .setValueSerializationSchema(ConfluentRegistryAvroSerializationSchema.forSpecific(FlightAvroData.class, topicName + "-value", producerProperties.getProperty("schema.registry.url"), registryConfigs))
            .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        KafkaSink<FlightAvroData> flightSink = KafkaSink.<FlightAvroData>builder()
            .setKafkaProducerConfig(producerProperties)
            .setTransactionalIdPrefix("avro-flight-data-") // unique per job
            .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
            .setRecordSerializer(flightSerializer)
            .build();

        /*
         * Defines the workflow for the Flink job graph (DAG) by connecting the data streams and
         * applying transformations to the data streams
         */
        consolidateFlightData(skyOneStream, sunsetStream)
            .sinkTo(flightSink)
            .name("flightdata_sink");

        try {
            // --- Execute the Flink job graph (DAG)
            env.execute("AvroFlightConsolidatorApp");
        } catch (Exception e) {
            LOGGER.error("The App stopped early due to the following: {}", e.getMessage(), e);
            throw e; // Rethrow the exception to signal failure.
        }
    }

    /**
     * This method consolidates the flight data from the two airlines.
     * 
     * @param skyOneSource - The datastream source for the `skyone_avro` Kafka topic
     * @param sunsetSource - The datastream source for the `sunset_avro` Kafka topic
     * @return the consolidate flight data into one datastream.
     */
	public static DataStream<FlightAvroData> consolidateFlightData(DataStream<AirlineAvroData> skyOneSource, DataStream<AirlineAvroData> sunsetSource) {
        DataStream<FlightAvroData> skyOneFlightStream = 
            skyOneSource
                .filter(flight -> isFutureFlight(flight))
                .map(flight -> transformToFlightData(flight, "SkyOne"))
                .name("skyone_flight_transform");

		DataStream<FlightAvroData> sunsetFlightStream = 
            sunsetSource
                .filter(flight -> isFutureFlight(flight))
                .map(flight -> transformToFlightData(flight, "Sunset"))
                .name("sunset_flight_transform");

		return skyOneFlightStream.union(sunsetFlightStream);
    }

    /**
     * Checks if a flight's arrival time is in the future.
     * Includes null safety and exception handling for malformed dates.
     * 
     * @param flight The airline flight data
     * @return true if the flight arrives in the future, false otherwise
     */
    private static boolean isFutureFlight(AirlineAvroData flight) {
        try {
            // --- Null check for flight and arrival time
            if (flight == null || flight.getArrivalTime() == null) {
                LOGGER.warn("Flight or arrival time is null, filtering out");
                return false;
            }

            LocalDateTime arrivalTime = LocalDateTime.parse(flight.getArrivalTime(), DATE_TIME_FORMATTER);
            return arrivalTime.isAfter(LocalDateTime.now());
        } catch (DateTimeParseException e) {
            LOGGER.warn("Failed to parse arrival time for flight {}: {}", 
                flight != null ? flight.getFlightNumber() : "unknown", 
                e.getMessage());
            return false;
        }
    }

    /**
     * Transforms airline-specific data into the unified FlightAvroData format.
     * 
     * @param flight The source airline data
     * @param airlineName The name of the airline
     * @return The transformed flight data
     */
    private static FlightAvroData transformToFlightData(AirlineAvroData flight, String airlineName) {
        FlightAvroData flightData = new FlightAvroData();

        flightData.setEmailAddress(flight.getEmailAddress());
        flightData.setDepartureTime(flight.getDepartureTime());
        flightData.setDepartureAirportCode(flight.getDepartureAirportCode());
        flightData.setArrivalTime(flight.getArrivalTime());
        flightData.setArrivalAirportCode(flight.getArrivalAirportCode());
        flightData.setFlightNumber(flight.getFlightNumber());
        flightData.setConfirmationCode(flight.getConfirmationCode());
        
        flightData.setAirline(airlineName);
        return flightData;
    }
}