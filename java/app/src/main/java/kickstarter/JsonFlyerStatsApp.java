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
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kickstarter.helper.SnakeCaseJsonDeserializationSchema;
import kickstarter.model.FlightJsonData;
import kickstarter.model.FlyerStatsJsonData;


public class JsonFlyerStatsApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonFlyerStatsApp.class);


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

        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `flight` with the
         * specified deserializer
         */
        KafkaSource<FlightJsonData> flightDataSource = 
            KafkaSource.<FlightJsonData>builder()
                .setProperties(consumerProperties)
                .setTopics("flight")
                .setGroupId("flight_group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SnakeCaseJsonDeserializationSchema<>(FlightJsonData.class))
                .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStreamSource<FlightJsonData> flightDataStream = 
            env.fromSource(flightDataSource, WatermarkStrategy.forMonotonousTimestamps(), "flightdata_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `flyer_stats` with the
         * specified serializer
         */
        KafkaRecordSerializationSchema<FlyerStatsJsonData> flyerStatsSerializer = 
            KafkaRecordSerializationSchema
                .<FlyerStatsJsonData>builder()
                .setTopic("flyer_stats")
                .setValueSerializationSchema(new JsonSerializationSchema<>(Common::getMapper))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        producerProperties.put("transaction.timeout.ms", "900000"); // 15m for long checkpoints
        producerProperties.put("enable.idempotence", "true");       // typically implied, explicit is fine
        KafkaSink<FlyerStatsJsonData> flyerStatsSink = 
            KafkaSink.<FlyerStatsJsonData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(flyerStatsSerializer)
                .setTransactionalIdPrefix("json-flyer-stats-") // unique per job
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .build();

        /*
         * Defines the workflow for the Flink job graph (DAG) by connecting the data streams and
         * applying transformations to the data streams
         */
        defineWorkflow(flightDataStream)
            .sinkTo(flyerStatsSink)
            .name("flyer_stats_sink")
            .uid("flyer_stats_sink");

        try {
            // --- Execute the Flink job graph (DAG)
            env.execute("JsonFlyerStatsApp");
        } catch (Exception e) {
            LOGGER.error("The App stopped early due to the following: ", e);
            throw e;
        }        
    }

    /**
     * This method defines the workflow for the Flink application.  It maps the data from the
     * `flight` Kafka topic to the `flyer_stats` Kafka topic.
     * 
     * @param flightDataSource the data stream from the `flight` Kafka topic.
     * @return the data stream to the `flyer_stats` Kafka topic.
     */
    public static DataStream<FlyerStatsJsonData> defineWorkflow(DataStream<FlightJsonData> flightDataSource) {
        return flightDataSource
            .map(FlyerStatsJsonData::new)
            .name("flight_to_flyer_stats_map").uid("flight_to_flyer_stats_map")
            .keyBy(FlyerStatsJsonData::getEmailAddress)
            .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
            .reduce(FlyerStatsJsonData::merge, new FlyerStatsJsonDataProcessWindowFunction())
            .name("flyer_stats_aggregation_window").uid("flyer_stats_aggregation_window");
    }
}