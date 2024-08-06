/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class processes data from the `flightdata` Kafka topic to aggregate user
 * statistics in the `userstatistics` Kafka topic.
 */
package apache_flink.kickstarter.datastream_api;

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
import java.time.*;
import java.util.*;
import org.slf4j.*;

import apache_flink.kickstarter.datastream_api.model.*;


public class UserStatisticsApp {
    private static final Logger logger = LoggerFactory.getLogger(UserStatisticsApp.class);


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        /*
		 * --- Kafka Consumer Config
		 * Retrieve the properties from the local properties files, or from AWS
         * Secrets Manager and AWS Systems Manager Parameter Store.  Then ingest
		 * properties into the Flink app
		 */
        DataStream<Properties> dataStreamConsumerProperties = env.addSource(new KafkaClientPropertiesSource(true, args));
		Properties consumerProperties = new Properties();
		dataStreamConsumerProperties
			.executeAndCollect()
				.forEachRemaining(typeValue -> {
					consumerProperties.putAll(typeValue);
				});

        /*
		 * --- Kafka Producer Config
		 * Retrieve the properties from the local properties files, or from AWS
         * Secrets Manager and AWS Systems Manager Parameter Store.  Then ingest
		 * properties into the Flink app
		 */
        DataStream<Properties> dataStreamProducerProperties = env.addSource(new KafkaClientPropertiesSource(false, args));
		Properties producerProperties = new Properties();
		dataStreamProducerProperties
			.executeAndCollect()
				.forEachRemaining(typeValue -> {
					producerProperties.putAll(typeValue);
				});

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
            env.execute("UserStatisticsApp");
        } catch (Exception e) {
            logger.error("The App stopped early due to the following: {}", e.getMessage());
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