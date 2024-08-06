/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.kickstarter.datastream_api;

import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.configuration.Configuration;
import java.io.*;
import java.util.*;

import apache_flink.enums.*;
import apache_flink.helper.*;



public class KafkaClientPropertiesSource extends RichSourceFunction<Properties>{
    public static final String FLAG_GET_FROM_AWS = "--get-from-aws";
    public static final String CONFLUENT_CLOUD_RESOURCE_PATH = "/confluent_cloud_resource/";
    public static final String KAFKA_CLUSTER_SECRETS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "kafka_cluster/java_client";
    public static final String SCHEMA_REGISTRY_CLUSTER_SECRETS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "schema_registry_cluster/java_client";
    public static final String KAFKA_CLIENT_CONSUMER_PARAMETERS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "consumer_kafka_client";
    public static final String KAFKA_CLIENT_PRODUCER_PARAMETERS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "producer_kafka_client";

    @SuppressWarnings("unused")
    private volatile boolean isRunning = true;

    private volatile String[] args;
    private volatile boolean consumerKafkaClient;


    public KafkaClientPropertiesSource(final boolean consumerKafkaClient, final String[] args) {
        this.consumerKafkaClient = consumerKafkaClient;
        this.args = args;        
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        // Any setup can be done here if needed
    }

    @Override
    public void run(SourceContext<Properties> ctx) throws Exception {
        ObjectResult<Properties> properties = getKafkaClientProperties(this.consumerKafkaClient, this.args);
		if(!properties.isSuccessful()) { 
			throw new RuntimeException("Failed to retrieve the Kafka Client properties could not be retrieved because " + properties.getErrorMessageCode() + " " + properties.getErrorMessage());
		}
        
        // Emit the properties to the downstream operators
        ctx.collect(properties.get());
        
        // Stop the source after emitting the data once
        isRunning = false;
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    /**
     * Loops through the `args` parameter and checks for the `FLAG_GET_FROM_AWS` flag.
     * 
     * @param args list of strings passed to the main method.
     * @return true if the flag is found, false otherwise.
     */
    public static boolean checkForFlagGetFromAws(final String[] args) {
        for (String arg:args) {
			if(arg.equalsIgnoreCase(FLAG_GET_FROM_AWS))
                return true;
		}
        return false;
    }

    /**
     * 
     * 
     * @param consumerKafkaClient
     * @param args
     * @return
     */
    private ObjectResult<Properties> getKafkaClientProperties(final boolean consumerKafkaClient, final String[] args) {
		if(checkForFlagGetFromAws(args)) {
			/*
			 * The flag was passed to the Job, and therefore the properties will be fetched
			 * from AWS Systems Manager Parameter Store and Secrets Manager, respectively.
			 */
            final String kakfaClientParametersPath = consumerKafkaClient ? KAFKA_CLIENT_CONSUMER_PARAMETERS_PATH : KAFKA_CLIENT_PRODUCER_PARAMETERS_PATH;
            
            final KafkaClient kafkaClient = new KafkaClient(KAFKA_CLUSTER_SECRETS_PATH, kakfaClientParametersPath);
            return kafkaClient.getKafkaClusterPropertiesFromAws();
		} else {
			/*
			 * The flag was NOT passed to the Job, therefore the all the properties will be
			 * fetched from the producer.properties file.
			 */
            try {
                Properties properties = new Properties();
                final String resourceFilename = consumerKafkaClient ? "consumer.properties" : "producer.properties";
                try (InputStream stream = KafkaClientPropertiesSource.class.getClassLoader().getResourceAsStream(resourceFilename)) {
                    properties.load(stream);
                }
                return new ObjectResult<>(properties);
            } catch (final IOException e) {
                return new ObjectResult<>(ErrorEnum.ERR_CODE_IO_EXCEPTION.getCode(), e.getMessage());
            }
		}        
    }
}
