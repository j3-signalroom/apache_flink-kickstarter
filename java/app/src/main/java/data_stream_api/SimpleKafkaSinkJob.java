package data_stream_api;

import java.io.*;
import java.util.*;
import javax.annotation.Nullable;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.*;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.*;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.*;
import org.slf4j.*;


public class SimpleKafkaSinkJob {
	private static Logger logger = LoggerFactory.getLogger(SimpleKafkaSinkJob.class);

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// --- Kafka Producer Config 
		Properties producerProperties = new Properties();
		try (InputStream stream = DataGeneratorJob.class.getClassLoader().getResourceAsStream("producer.properties")) {
			producerProperties.load(stream);
		}

        // --- 
        GeneratorFunction<Long, Long> generatorFunction = index -> index;

        DataGeneratorSource<Long> source =
            new DataGeneratorSource<>(
                generatorFunction,
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(10),
                Types.LONG);

        DataStreamSource<Long> stream =
            env.fromSource(source, WatermarkStrategy.noWatermarks(), "Generator Source");

        KafkaRecordSerializationSchema<Long> serializationSchema = 
			KafkaRecordSerializationSchema.<Long>builder()
				.setTopic("flink_sink")
				.setValueSerializationSchema(new JsonSerializationSchema<>(SimpleKafkaSinkJob::getMapper))
				.build();

        KafkaSink<Long> flinkSink = 
			KafkaSink.<Long>builder()
				.setKafkaProducerConfig(producerProperties)
				.setRecordSerializer(serializationSchema)
				.setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
				.build();

        stream.sinkTo(flinkSink).name("flink_sink_example");

		try {
			env.execute("SimpleKafkaSink");
		} catch(final Exception e) {
            logger.error("The Job stopped early due to the following: {}", e.getMessage());
		}
        
    }

	// Custom KafkaSerializationSchema with custom partitioning logic
    public static class CustomKafkaSerializationSchema implements KafkaSerializationSchema<String> {
        private final String topic;

        public CustomKafkaSerializationSchema(String topic) {
            this.topic = topic;
        }

        @Override
        public ProducerRecord<byte[], byte[]> serialize(String element, @Nullable Long timestamp) {
            // Custom partition logic: Here we send all records to partition 0
            int partition = 0; // you can implement your custom logic here

            return new ProducerRecord<>(topic, partition, null, element.getBytes());
        }
    }
	
    private static ObjectMapper getMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}
