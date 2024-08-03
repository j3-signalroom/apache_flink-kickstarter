/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.kickstarter.datastream_api;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.*;
import java.util.*;
import org.slf4j.*;

import apache_flink.*;
import apache_flink.kickstarter.datastream_api.model.*;
import apache_flink.helper.*;


public class DataGeneratorJob {
	private static final Logger logger = LoggerFactory.getLogger(DataGeneratorJob.class);


	public static void main(String[] args) throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// --- Kafka Producer Config
		ObjectResult<Properties> producerProperties = Common.getKafkaClientProperties(false, args);
		if(!producerProperties.isSuccessful()) {
			logger.error("The Producer Kafka Client properties could not be retrieved because {} {}", producerProperties.getErrorMessageCode(), producerProperties.getErrorMessage());
			System.exit(1);
		}
        
		DataGeneratorSource<SkyOneAirlinesFlightData> skyOneSource =
			new DataGeneratorSource<>(
				index -> DataGenerator.generateSkyOneAirlinesFlightData(),
				Long.MAX_VALUE,
				RateLimiterStrategy.perSecond(1),
				Types.POJO(SkyOneAirlinesFlightData.class)
			);

		DataStream<SkyOneAirlinesFlightData> skyOneStream = env.fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

		KafkaRecordSerializationSchema<SkyOneAirlinesFlightData> skyOneSerializer = 
			KafkaRecordSerializationSchema.<SkyOneAirlinesFlightData>builder()
				.setTopic("skyone")
				.setValueSerializationSchema(new JsonSerializationSchema<>(DataGeneratorJob::getMapper))
				.build();

		KafkaSink<SkyOneAirlinesFlightData> skyOneSink = 
			KafkaSink.<SkyOneAirlinesFlightData>builder()
				.setKafkaProducerConfig(producerProperties.get())
				.setRecordSerializer(skyOneSerializer)
				.setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
				.build();

		skyOneStream.sinkTo(skyOneSink).name("skyone_sink");

		DataGeneratorSource<SunsetAirFlightData> sunsetSource =
			new DataGeneratorSource<>(
				index -> DataGenerator.generateSunsetAirFlightData(),
				Long.MAX_VALUE,
				RateLimiterStrategy.perSecond(1),
				Types.POJO(SunsetAirFlightData.class)
			);

		DataStream<SunsetAirFlightData> sunsetStream = env.fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

		KafkaRecordSerializationSchema<SunsetAirFlightData> sunSetSerializer = 
			KafkaRecordSerializationSchema.<SunsetAirFlightData>builder()
				.setTopic("sunset")
				.setValueSerializationSchema(new JsonSerializationSchema<>(DataGeneratorJob::getMapper))
				.build();

		KafkaSink<SunsetAirFlightData> sunsetSink = 
			KafkaSink.<SunsetAirFlightData>builder()
				.setKafkaProducerConfig(producerProperties.get())
				.setRecordSerializer(sunSetSerializer)
				.setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
				.build();

		sunsetStream.sinkTo(sunsetSink).name("sunset_sink");

		try {
			env.execute("DataGeneratorJob");
		} catch (Exception e) {
			logger.error("The Job stopped early due to the following: {}", e.getMessage());
		}
	}

	private static ObjectMapper getMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}