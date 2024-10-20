/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class processes data from the `airline.flight` Kafka topic to aggregate user
 * statistics in the `airline.flyer_stats` Kafka topic.
 */
package kickstarter;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.kafka.clients.producer.ProducerConfig;
import java.time.*;
import java.util.*;
import org.slf4j.*;

import kickstarter.model.*;


public class FlyerStatsApp {
    private static final Logger logger = LoggerFactory.getLogger(FlyerStatsApp.class);


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
		dataStreamConsumerProperties.executeAndCollect()
                                    .forEachRemaining(typeValue -> {
                                        consumerProperties.putAll(typeValue);
                                    });

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
		try {
		    dataStreamProducerProperties
                .executeAndCollect()
                .forEachRemaining(typeValue -> {
                    producerProperties.putAll(typeValue);
                });
        } catch (final Exception e) {
            System.out.println("The Flink App stopped during the reading of the custom data source stream because of the following: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
		}

        producerProperties.setProperty(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, "60000");

        // ---Configure ObjectMapper to ignore unknown properties
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        /*
         * Sets up a Flink Kafka source to consume data from the Kafka topic `airline.flight` with the
         * specified deserializer
         */
        KafkaSource<FlightData> flightDataSource = 
            KafkaSource.<FlightData>builder()
                .setProperties(consumerProperties)
                .setTopics("airline.flight")
                .setGroupId("flight_group")
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
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.flyer_stats` with the
         * specified serializer
         */
        KafkaRecordSerializationSchema<FlyerStatsData> flyerStatsSerializer = 
            KafkaRecordSerializationSchema
                .<FlyerStatsData>builder()
                .setTopic("airline.flyer_stats")
                .setValueSerializationSchema(new JsonSerializationSchema<>(() -> new ObjectMapper().registerModule(new JavaTimeModule())))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG)
         */
        KafkaSink<FlyerStatsData> flyerStatsSink = 
            KafkaSink.<FlyerStatsData>builder()
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
            env.execute("FlyerStatsApp");
        } catch (Exception e) {
            logger.error("The App stopped early due to the following: {}", e.getMessage());
        }        
    }

    /**
     * This method defines the workflow for the Flink application.  It maps the data from the
     * `airline.flight` Kafka topic to the `airline.flyer_stats` Kafka topic.
     * 
     * @param flightDataSource the data stream from the `airline.flight` Kafka topic.
     * @return the data stream to the `airline.flyer_stats` Kafka topic.
     */
    public static DataStream<FlyerStatsData> defineWorkflow(DataStream<FlightData> flightDataSource) {
        return flightDataSource
            .map(FlyerStatsData::new)
            .keyBy(FlyerStatsData::getEmailAddress)
            .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
            .reduce(FlyerStatsData::merge, new ProcessFlyerStatsDataFunction());
    }
}