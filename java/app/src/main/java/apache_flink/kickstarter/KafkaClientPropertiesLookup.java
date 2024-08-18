/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * An Apache Flink custom source data stream is a user-defined source of data that
 * is integrated into a Flink application to read and process data from non-standard
 * or custom sources. This custom source can be anything that isn't supported by Flink
 * out of the box, such as proprietary REST APIs, specialized databases, custom hardware 
 * interfaces, etc. J3 utilizes a Custom Source Data Stream to read the AWS Secrets Manager 
 * secrets and AWS Systems Manager Parameter Store properties during the initial start of a 
 * App, then caches the properties for use by any subsequent events that need these properties.
 */
package apache_flink.kickstarter;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import java.util.concurrent.atomic.AtomicReference;
import java.io.*;
import java.util.*;

import apache_flink.kickstarter.enums.*;
import apache_flink.kickstarter.helper.*;


public class KafkaClientPropertiesLookup extends RichMapFunction<Object, Properties>{
    private static final String CONFLUENT_CLOUD_RESOURCE_PATH = "/confluent_cloud_resource/";
    private static final String KAFKA_CLUSTER_SECRETS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "kafka_cluster/java_client";
    private static final String KAFKA_CLIENT_CONSUMER_PARAMETERS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "consumer_kafka_client";
    private static final String KAFKA_CLIENT_PRODUCER_PARAMETERS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "producer_kafka_client";

    private transient AtomicReference<Properties> properties;


    /**
     * This method is called when the source is opened.  Any setup can be done
     * here if needed.
     * 
     * @parameters The configuration containing the parameters attached to the
     * contract.
     * @throws Exception - Implementations may forward exceptions, which are caught
     * by the runtime.  When the runtime catches an exception, it aborts the task and 
     * lets the fail-over logic decide whether to retry the task execution.
     */
    @Override
    public void open(Configuration configuration) throws Exception {
        // ---
        boolean consumerKafkaClient = configuration.get(KafkaClientPropertiesLookupConfigOptions.JOB_USE_AWS);
        boolean useAws = configuration.get(KafkaClientPropertiesLookupConfigOptions.JOB_FOR_CONSUMER_KAFKA_CLIENT);

        // ---
        ObjectResult<Properties> properties = getKafkaClientProperties(consumerKafkaClient, useAws);
		if(!properties.isSuccessful()) { 
			throw new RuntimeException("Failed to retrieve the Kafka Client properties could not be retrieved because " + properties.getErrorMessageCode() + " " + properties.getErrorMessage());
		}

        // --- 
        this.properties = new AtomicReference<>(properties.get());
    }

    @Override
    public Properties map(Object value) {
        return(this.properties.get());
    }
    
    @Override
    public void close() throws Exception {}

    /**
     * This method retrieves the Kafka Client properties from either the local properties files
     * or AWS Secrets Manager and AWS Systems Manager Parameter Store.
     * 
     * @param consumerKafkaClient true if the Kafka Client is a consumer, false if the Kafka Client
     * is a producer.
     * @param useAws true if the properties should be retrieved from AWS, false if the properties.
     * @return the Kafka Client properties from the local properties file if no arugments are passed,
     * or from AWS Secrets Manager and AWS Systems Manager Parameter Store if --get-from-aws is passed
     * as an argument.  Otherwise, an error message occurs, an error code and message is returned.
     */
    private ObjectResult<Properties> getKafkaClientProperties(final boolean consumerKafkaClient, final boolean useAws) {
		if(!useAws) {
			/*
			 * The flag was passed to the App, and therefore the properties will be fetched
			 * from AWS Systems Manager Parameter Store and Secrets Manager, respectively.
			 */
            final String kakfaClientParametersPath = consumerKafkaClient ? KAFKA_CLIENT_CONSUMER_PARAMETERS_PATH : KAFKA_CLIENT_PRODUCER_PARAMETERS_PATH;
            
            final KafkaClient kafkaClient = new KafkaClient(KAFKA_CLUSTER_SECRETS_PATH, kakfaClientParametersPath);
            return kafkaClient.getKafkaClusterPropertiesFromAws();
		} else {
			/*
			 * The flag was NOT passed to the App, therefore the properties will be fetched
			 * from a local properties file.
			 */
            try {
                Properties properties = new Properties();
                final String resourceFilename = consumerKafkaClient ? "consumer.properties" : "producer.properties";
                try (InputStream stream = KafkaClientPropertiesLookup.class.getClassLoader().getResourceAsStream(resourceFilename)) {
                    properties.load(stream);
                }
                return new ObjectResult<>(properties);
            } catch (final IOException e) {
                return new ObjectResult<>(ErrorEnum.ERR_CODE_IO_EXCEPTION.getCode(), e.getMessage());
            }
		}        
    }
}
