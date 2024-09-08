/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class processes data from the `airline.all` Kafka topic to aggregate user
 * statistics in the `airline.user_statistics` Kafka topic.
 */
package apache_flink.kickstarter;

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

import apache_flink.kickstarter.model.*;


public class UserStatisticsApp {
    private static final Logger logger = LoggerFactory.getLogger(UserStatisticsApp.class);


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
        // --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG)
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

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
         * Sets up a Flink Kafka source to consume data from the Kafka topic `airline.all` with the
         * specified deserializer
         */
        KafkaSource<FlightData> flightDataSource = 
            KafkaSource.<FlightData>builder()
                .setProperties(consumerProperties)
                .setTopics("airline.all")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new JsonDeserializationSchema<>(FlightData.class))
                .build();

        /*
         * Takes the results of the Kafka source and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        DataStreamSource<FlightData> flightDataStream = 
            env.fromSource(flightDataSource, WatermarkStrategy.forMonotonousTimestamps(), "flightdata_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.user_statistics` with the
         * specified serializer
         */
        KafkaRecordSerializationSchema<UserStatisticsData> statisticsSerializer = 
            KafkaRecordSerializationSchema
                .<UserStatisticsData>builder()
                .setTopic("airline.user_statistics")
                .setValueSerializationSchema(new JsonSerializationSchema<>(() -> new ObjectMapper().registerModule(new JavaTimeModule())))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        KafkaSink<UserStatisticsData> statsSink = 
            KafkaSink.<UserStatisticsData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(statisticsSerializer)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        /*
         * Defines the workflow for the Flink job graph (DAG) by connecting the data streams and
         * applying transformations to the data streams
         */
        defineWorkflow(flightDataStream)
                        .sinkTo(statsSink)
                        .name("userstatistics_sink")
                        .uid("userstatistics_sink");

        try {
            // --- Execute the Flink job graph (DAG)
            env.execute("UserStatisticsApp");
        } catch (Exception e) {
            logger.error("The App stopped early due to the following: {}", e.getMessage());
        }        
    }

    /**
     * This method defines the workflow for the Flink application.  It maps the data from the
     * `airline.all` Kafka topic to the `airline.user_statistics` Kafka topic.
     * 
     * @param flightDataSource the data stream from the `airline.all` Kafka topic.
     * @return the data stream to the `airline.user_statistics` Kafka topic.
     */
    public static DataStream<UserStatisticsData> defineWorkflow(DataStream<FlightData> flightDataSource) {
        return flightDataSource
            .map(UserStatisticsData::new)
            .keyBy(UserStatisticsData::getEmailAddress)
            .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
            .reduce(UserStatisticsData::merge, new ProcessUserStatisticsDataFunction());
    }
}