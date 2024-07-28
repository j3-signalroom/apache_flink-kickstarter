package data_stream_api;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import java.io.*;
import java.time.*;
import java.util.*;
import org.slf4j.*;

import data_stream_api.model.*;


public class UserStatisticsJob {
    private static final Logger logger = LoggerFactory.getLogger(UserStatisticsJob.class);


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // --- Kafka Consumer Config
        Properties consumerProperties = new Properties();
        try (InputStream stream = DataGeneratorJob.class.getClassLoader().getResourceAsStream("consumer.properties")) {
			consumerProperties.load(stream);
		}

        // --- Kafka Producer Config
        Properties producerProperties = new Properties();
        try (InputStream stream = DataGeneratorJob.class.getClassLoader().getResourceAsStream("producer.properties")) {
			producerProperties.load(stream);
		}

        KafkaSource<FlightData> flightDataSource = 
            KafkaSource.<FlightData>builder()
                .setProperties(consumerProperties)
                .setTopics("flightdata")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new JsonDeserializationSchema<>(FlightData.class))
                .build();

        DataStreamSource<FlightData> flightDataStream = 
            env.fromSource(flightDataSource, WatermarkStrategy.forMonotonousTimestamps(), "flightdata_source");

        KafkaRecordSerializationSchema<UserStatisticsData> statisticsSerializer = 
            KafkaRecordSerializationSchema
                .<UserStatisticsData>builder()
                .setTopic("userstatistics")
                .setValueSerializationSchema(new JsonSerializationSchema<>(() -> new ObjectMapper().registerModule(new JavaTimeModule())))
                .build();

        KafkaSink<UserStatisticsData> statsSink = 
            KafkaSink.<UserStatisticsData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(statisticsSerializer)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        defineWorkflow(flightDataStream)
                        .sinkTo(statsSink)
                        .name("userstatistics_sink")
                        .uid("userstatistics_sink");

        try {
            env.execute("UserStatisticsData");
        } catch (Exception e) {
            logger.error("The Job stopped early due to the following: {}", e.getMessage());
        }        
    }

    public static DataStream<UserStatisticsData> defineWorkflow(DataStream<FlightData> flightDataSource) {
        return flightDataSource
            .map(UserStatisticsData::new)
            .keyBy(UserStatisticsData::getEmailAddress)
            .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
            .reduce(UserStatisticsData::merge, new ProcessUserStatisticsDataFunction());
    }
}