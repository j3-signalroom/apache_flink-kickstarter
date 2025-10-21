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

import org.apache.flink.formats.avro.registry.confluent.*;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.connector.kafka.source.*;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;


import kickstarter.model.*;


public class AvroFlyerStatsApp {
    private static final Logger logger = Logger.getLogger(AvroFlyerStatsApp.class.getName());


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
                .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                .build();

        /*
         * Calculates and aggregates flight statistics for each flyer, such as the total flight duration and 
         * number of flights they have taken, within one-minute tumbling windows.
         */
        flightDataStream
            .map(flightdata -> 
                {
                    FlyerStatsAvroData flyerStats = new FlyerStatsAvroData();

                    flyerStats.setEmailAddress(flightdata.getEmailAddress());
                    flyerStats.setTotalFlightDuration(Duration.between(LocalDateTime.parse(flightdata.getDepartureTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                                                      LocalDateTime.parse(flightdata.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).toMinutes());
                    flyerStats.setNumberOfFlights(1);
                    
                    return flyerStats;
            })
            .keyBy(flightdata -> flightdata.getEmailAddress())
            .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
            .reduce(((flyerStats1, flyerStats2) -> {
                flyerStats1.setTotalFlightDuration(flyerStats1.getTotalFlightDuration() + flyerStats2.getTotalFlightDuration());
                flyerStats1.setNumberOfFlights(flyerStats1.getNumberOfFlights() + flyerStats2.getNumberOfFlights());

                return flyerStats1;
            }), new FlyerStatsAvroDataProcessWindowFunction())
            .sinkTo(flyerStatsSink)
            .name("flyer_stats_sink")
            .uid("flyer_stats_sink");

        try {
            // --- Execute the Flink job graph (DAG)
            env.execute("AvroFlyerStatsApp");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "The App stopped early due to the following: ", e.getMessage());
        }        
    }
}