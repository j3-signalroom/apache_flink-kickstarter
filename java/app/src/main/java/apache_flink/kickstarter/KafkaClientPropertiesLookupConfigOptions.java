package apache_flink.kickstarter;


import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ConfigOption;

public class KafkaClientPropertiesLookupConfigOptions {
    public static final ConfigOption<Boolean> JOB_USE_AWS = 
        ConfigOptions.key("job.use_aws")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Flag to determine if the App should use AWS Secrets Manager and AWS Systems Manager Parameter Store to retrieve the Kafka Client properties.");

    public static final ConfigOption<Boolean> JOB_FOR_CONSUMER_KAFKA_CLIENT = 
        ConfigOptions.key("job.for_consumer_kafka_client")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Flag to determine if the App should get Cusumer or Producer Kafka properties.");
}
