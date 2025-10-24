/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class processes data from the `flight` Kafka topic to aggregate user
 * statistics in the `flyer_stats` Kafka topic.
 */
package kickstarter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Properties;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kickstarter.model.FlightAvroData;
import kickstarter.model.FlyerStatsAvroData;


public class AvroFlyerStatsApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(AvroFlyerStatsApp.class);

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

        // --- Kafka Consumer and Producer Client Properties
        Properties consumerProperties = Common.collectConfluentProperties(env, serviceAccountUser, true);
        Properties producerProperties = Common.collectConfluentProperties(env, serviceAccountUser, false);

        // --- Retrieve the schema registry properties and store it in a map.
        Map<String, String> registryConfigs = Common.extractRegistryConfigs(producerProperties);

        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `flight_avro` with the
         * specified deserializer
         */
        KafkaSource<FlightAvroData> flightDataSource = 
            KafkaSource.<FlightAvroData>builder()
                .setProperties(consumerProperties)
                .setTopics("flight_avro")
                .setGroupId("flight_group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(ConfluentRegistryAvroDeserializationSchema.forSpecific(FlightAvroData.class, producerProperties.getProperty("schema.registry.url"), registryConfigs))
                .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStreamSource<FlightAvroData> flightDataStream = 
            env.fromSource(flightDataSource, WatermarkStrategy.forMonotonousTimestamps(), "flightdata_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `flyer_stats` with the
         * specified serializer
         */
        final String topicName = "flyer_stats_avro";
        KafkaRecordSerializationSchema<FlyerStatsAvroData> flyerStatsSerializer = 
            KafkaRecordSerializationSchema
                .<FlyerStatsAvroData>builder()
                .setTopic(topicName)
                .setValueSerializationSchema(ConfluentRegistryAvroSerializationSchema.forSpecific(FlyerStatsAvroData.class, topicName + "-value", producerProperties.getProperty("schema.registry.url"), registryConfigs))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        KafkaSink<FlyerStatsAvroData> flyerStatsSink = 
            KafkaSink.<FlyerStatsAvroData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(flyerStatsSerializer)
                .setTransactionalIdPrefix("avro-flyer-stats-") // unique per job
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .build();

        /*
         * Calculates and aggregates flight statistics for each flyer, such as the total flight duration and 
         * number of flights they have taken, within one-minute tumbling windows.
         */
        DataStream<FlyerStatsAvroData> flyerStatsStream = flightDataStream
            .filter(flight -> isValidFlight(flight))
            .name("filter_valid_flights")
            .map(flightdata -> transformToFlyerStats(flightdata))
            .name("transform_to_flyer_stats")
            .keyBy(flyerStats -> flyerStats.getEmailAddress())
            .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
            .reduce(((flyerStats1, flyerStats2) -> {
                flyerStats1.setTotalFlightDuration(flyerStats1.getTotalFlightDuration() + flyerStats2.getTotalFlightDuration());
                flyerStats1.setNumberOfFlights(flyerStats1.getNumberOfFlights() + flyerStats2.getNumberOfFlights());
                return flyerStats1;
            }), new FlyerStatsAvroDataProcessWindowFunction())
            .name("aggregate_flyer_stats");

        // --- Sink the aggregated stats to Kafka
        flyerStatsStream
            .sinkTo(flyerStatsSink)
            .name("flyer_stats_sink")
            .uid("flyer_stats_sink");

        try {
            // --- Execute the Flink job graph (DAG)
            env.execute("AvroFlyerStatsApp");
        } catch (Exception e) {
            LOGGER.error( "The App stopped early due to the following: {}", e.getMessage(), e);
            throw e; // Rethrow the exception after logging
        }        
    }

    /**
     * Validates that a flight has all required fields populated.
     * 
     * @param flight The flight data to validate
     * @return true if the flight is valid, false otherwise
     */
    private static boolean isValidFlight(FlightAvroData flight) {
        if (flight == null) {
            LOGGER.warn("Flight is null, filtering out");
            return false;
        }
        
        if (flight.getDepartureTime() == null || flight.getArrivalTime() == null) {
            LOGGER.warn("Flight has null departure or arrival time, filtering out");
            return false;
        }
        
        if (flight.getEmailAddress() == null) {
            LOGGER.warn("Flight has null email address, filtering out");
            return false;
        }
        
        return true;
    }

    /**
     * Transforms flight data into flyer statistics by calculating the flight duration.
     * Includes exception handling for malformed dates.
     * 
     * @param flightdata The flight data to transform
     * @return The flyer statistics data, or null if parsing fails
     */
    private static FlyerStatsAvroData transformToFlyerStats(FlightAvroData flightdata) {
        try {
            LocalDateTime departureTime = LocalDateTime.parse(
                flightdata.getDepartureTime(), DATE_TIME_FORMATTER
            );
            LocalDateTime arrivalTime = LocalDateTime.parse(
                flightdata.getArrivalTime(), DATE_TIME_FORMATTER
            );
            
            long flightDurationMinutes = Duration.between(departureTime, arrivalTime).toMinutes();
            
            FlyerStatsAvroData flyerStats = new FlyerStatsAvroData();
            flyerStats.setEmailAddress(flightdata.getEmailAddress());
            flyerStats.setTotalFlightDuration(flightDurationMinutes);
            flyerStats.setNumberOfFlights(1);
            
            return flyerStats;
        } catch (DateTimeParseException e) {
            LOGGER.warn("Failed to parse flight times for email {}: {}", 
                flightdata.getEmailAddress(), 
                e.getMessage());

            // --- Return a stats object with zero duration to avoid breaking the stream
            FlyerStatsAvroData flyerStats = new FlyerStatsAvroData();
            flyerStats.setEmailAddress(flightdata.getEmailAddress());
            flyerStats.setTotalFlightDuration(0L);
            flyerStats.setNumberOfFlights(0);
            return flyerStats;
        }
    }
}