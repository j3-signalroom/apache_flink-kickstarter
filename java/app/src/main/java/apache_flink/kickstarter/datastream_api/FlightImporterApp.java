/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class imports flight data from `airline.sunset` and `airline.skyone` Kafka topics
 * and converts it to a unified format for the `airline.all_airlines` Kafka topic.
 */
package apache_flink.kickstarter.datastream_api;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import java.util.*;
import java.time.*;
import org.slf4j.*;

import apache_flink.kickstarter.datastream_api.model.*;


public class FlightImporterApp {
    private static final Logger logger = LoggerFactory.getLogger(FlightImporterApp.class);


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
    @SuppressWarnings("rawtypes")
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

        @SuppressWarnings("unchecked")
        KafkaSource<SkyOneAirlinesFlightData> skyOneSource = KafkaSource.<SkyOneAirlinesFlightData>builder()
            .setProperties(consumerProperties)
            .setTopics("airline.skyone")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema(SkyOneAirlinesFlightData.class))
            .build();

        DataStream<SkyOneAirlinesFlightData> skyOneStream = env
            .fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

		@SuppressWarnings("unchecked")
        KafkaSource<SunsetAirFlightData> sunsetSource = KafkaSource.<SunsetAirFlightData>builder()
            .setProperties(consumerProperties)
            .setTopics("airline.sunset")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema(SunsetAirFlightData.class))
            .build();

        DataStream<SunsetAirFlightData> sunsetStream = env
            .fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

		KafkaRecordSerializationSchema<FlightData> flightSerializer = KafkaRecordSerializationSchema.<FlightData>builder()
            .setTopic("airline.all_airlines")
			.setValueSerializationSchema(new JsonSerializationSchema<FlightData>(FlightImporterApp::getMapper))
            .build();

        KafkaSink<FlightData> flightSink = KafkaSink.<FlightData>builder()
            .setKafkaProducerConfig(producerProperties)
            .setRecordSerializer(flightSerializer)
            .build();

        defineWorkflow(skyOneStream, sunsetStream)
            .sinkTo(flightSink)
            .name("flightdata_sink");

        try {
            env.execute("FlightImporterApp");
        } catch (Exception e) {
            logger.error("The App stopped early due to the following: {}", e.getMessage());
        }
    }

	public static DataStream<FlightData> defineWorkflow(DataStream<SkyOneAirlinesFlightData> skyOneSource, DataStream<SunsetAirFlightData> sunsetSource) {
        DataStream<FlightData> skyOneFlightStream =  skyOneSource
			.filter(flight -> flight.getFlightArrivalTime().isAfter(ZonedDateTime.now()))
			.map(SkyOneAirlinesFlightData::toFlightData);

		DataStream<FlightData> sunsetFlightStream = sunsetSource
            .filter(flight -> flight.getArrivalTime().isAfter(ZonedDateTime.now()))
            .map(flight -> flight.toFlightData());

		return skyOneFlightStream.union(sunsetFlightStream);
    }

	private static ObjectMapper getMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}