/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class processes data from the `flight` Kafka topic to aggregate user
 * statistics in the `flyer_stats` Kafka topic.
 */
package kickstarter;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import java.time.*;
import java.util.*;
import org.slf4j.*;

import kickstarter.helper.SnakeCaseJsonDeserializationSchema;
import kickstarter.model.*;


public class JsonFlyerStatsApp {
    private static final Logger logger = LoggerFactory.getLogger(JsonFlyerStatsApp.class);


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

        // --- Kafka Consumer and Producer Client Properties
        Properties consumerProperties = Common.collectConfluentProperties(env, serviceAccountUser, true);
        Properties producerProperties = Common.collectConfluentProperties(env, serviceAccountUser, false);

        // ---Configure ObjectMapper to ignore unknown properties
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
                .setValueSerializationSchema(new JsonSerializationSchema<FlyerStatsJsonData>(Common::getMapper))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        KafkaSink<FlyerStatsJsonData> flyerStatsSink = 
            KafkaSink.<FlyerStatsJsonData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(flyerStatsSerializer)
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
            logger.error("The App stopped early due to the following: {}", e.getMessage());
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
            .keyBy(FlyerStatsJsonData::getEmailAddress)
            .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
            .reduce(FlyerStatsJsonData::merge, new FlyerStatsJsonDataProcessWindowFunction());
    }
}