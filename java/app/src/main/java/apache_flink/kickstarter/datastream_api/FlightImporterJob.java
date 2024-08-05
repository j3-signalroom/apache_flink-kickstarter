/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
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


public class FlightImporterJob {
    private static final Logger logger = LoggerFactory.getLogger(FlightImporterJob.class);

    @SuppressWarnings("rawtypes")
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        /*
		 * --- Kafka Consumer Config
		 * Add the Producder Properties Source to the environment, and then
		 * extract the properties from the data stream
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
		 * Add the Producder Properties Source to the environment, and then
		 * extract the properties from the data stream
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
            .setTopics("skyone")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema(SkyOneAirlinesFlightData.class))
            .build();

        DataStream<SkyOneAirlinesFlightData> skyOneStream = env
            .fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

		@SuppressWarnings("unchecked")
        KafkaSource<SunsetAirFlightData> sunsetSource = KafkaSource.<SunsetAirFlightData>builder()
            .setProperties(consumerProperties)
            .setTopics("sunset")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema(SunsetAirFlightData.class))
            .build();

        DataStream<SunsetAirFlightData> sunsetStream = env
            .fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

		KafkaRecordSerializationSchema<FlightData> flightSerializer = KafkaRecordSerializationSchema.<FlightData>builder()
            .setTopic("flightdata")
			.setValueSerializationSchema(new JsonSerializationSchema<FlightData>(FlightImporterJob::getMapper))
            .build();

        KafkaSink<FlightData> flightSink = KafkaSink.<FlightData>builder()
            .setKafkaProducerConfig(producerProperties)
            .setRecordSerializer(flightSerializer)
            .build();

        defineWorkflow(skyOneStream, sunsetStream)
            .sinkTo(flightSink)
            .name("flightdata_sink");

        try {
            env.execute("FlightImporterJob");
        } catch (Exception e) {
            logger.error("The Job stopped early due to the following: {}", e.getMessage());
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