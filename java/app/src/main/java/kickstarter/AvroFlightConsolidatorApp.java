/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
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

import org.apache.flink.formats.avro.registry.confluent.*;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.connector.kafka.source.*;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import java.util.*;
import java.time.*;
import java.time.format.*;
import org.slf4j.*;

import kickstarter.model.*;


public class AvroFlightConsolidatorApp {
    private static final Logger logger = LoggerFactory.getLogger(AvroFlightConsolidatorApp.class);
    

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
            .setRecordSerializer(flightSerializer)
            .build();

        /*
         * Defines the workflow for the Flink job graph (DAG) by connecting the data streams and
         * applying transformations to the data streams
         */
        consolidatesFlightData(skyOneStream, sunsetStream)
            .sinkTo(flightSink)
            .name("flightdata_sink");

        try {
            // --- Execute the Flink job graph (DAG)
            env.execute("AvroFlightConsolidatorApp");
        } catch (Exception e) {
            logger.error("The App stopped early due to the following: {}", e.getMessage());
        }
    }

    /**
     * This method consolidates the flight data from the two airlines.
     * 
     * @param skyOneSource - The datastream source for the `skyone_avro` Kafka topic
     * @param sunsetSource - The datastream source for the `sunset_avro` Kafka topic
     * @return the consolidate flight data into one datastream.
     */
	public static DataStream<FlightAvroData> consolidatesFlightData(DataStream<AirlineAvroData> skyOneSource, DataStream<AirlineAvroData> sunsetSource) {
        DataStream<FlightAvroData> skyOneFlightStream = 
            skyOneSource
                .filter(flight -> LocalDateTime.parse(flight.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).isAfter(LocalDateTime.now()))
                .map(flight -> 
                    {
                        FlightAvroData flightData = new FlightAvroData();

                        flightData.setEmailAddress(flight.getEmailAddress());
                        flightData.setDepartureTime(flight.getDepartureTime());
                        flightData.setDepartureAirportCode(flight.getDepartureAirportCode());
                        flightData.setArrivalTime(flight.getArrivalTime());
                        flightData.setArrivalAirportCode(flight.getArrivalAirportCode());
                        flightData.setFlightNumber(flight.getFlightNumber());
                        flightData.setConfirmationCode(flight.getConfirmationCode());
                        
                        flightData.setAirline("SkyOne");
                        return flightData;
                    });

		DataStream<FlightAvroData> sunsetFlightStream = 
            sunsetSource
                .filter(flight -> LocalDateTime.parse(flight.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).isAfter(LocalDateTime.now()))
                .map(flight -> 
                    {
                        FlightAvroData flightData = new FlightAvroData();

                        flightData.setEmailAddress(flight.getEmailAddress());
                        flightData.setDepartureTime(flight.getDepartureTime());
                        flightData.setDepartureAirportCode(flight.getDepartureAirportCode());
                        flightData.setArrivalTime(flight.getArrivalTime());
                        flightData.setArrivalAirportCode(flight.getArrivalAirportCode());
                        flightData.setFlightNumber(flight.getFlightNumber());
                        flightData.setConfirmationCode(flight.getConfirmationCode());
                        
                        flightData.setAirline("Sunset");
                        return flightData;
                    });

		return skyOneFlightStream.union(sunsetFlightStream);
    }
}