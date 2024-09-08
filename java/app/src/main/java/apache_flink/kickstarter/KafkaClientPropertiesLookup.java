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


public class KafkaClientPropertiesLookup extends RichMapFunction<Properties, Properties>{
    private static final String CONFLUENT_CLOUD_RESOURCE_PATH = "/confluent_cloud_resource/";
    private static final String KAFKA_CLUSTER_SECRETS_PATH = "/kafka_cluster/java_client";
    private static final String KAFKA_CLIENT_CONSUMER_PARAMETERS_PATH = "/consumer_kafka_client";
    private static final String KAFKA_CLIENT_PRODUCER_PARAMETERS_PATH = "/producer_kafka_client";

    private transient AtomicReference<Properties> _properties;
    private volatile boolean _consumerKafkaClient;
    private volatile boolean _useAws;
    private volatile String _serviceAccountUser;


    /**
     * Default constructor.
     * 
     * @param consumerKafkaClient
     * @param appOptions
     * @throws Exception - Exception occurs when the service account user is empty.
     */
    public KafkaClientPropertiesLookup(final boolean consumerKafkaClient, final AppOptions appOptions) throws Exception {
        // ---  Set the fields
        this._consumerKafkaClient = consumerKafkaClient;
        this._useAws = appOptions.isGetFromAws();
        this._serviceAccountUser = appOptions.getServiceAccountUser();

        // --- Check if the service account user is empty, only if the --get-from-aws option is passed
        if(this._useAws)
            if(this._serviceAccountUser.isEmpty()) {
                throw new Exception("The service account user must be provided when the --get-from-aws option is passed.");
            }
    }

    /**
     * This method is called once per parallel task instance when the job starts. 
     * 
     * @parameters The configuration containing the parameters attached to the
     * contract.
     * @throws Exception - Implementations may forward exceptions, which are caught
     * by the runtime.  When the runtime catches an exception, it aborts the task and 
     * lets the fail-over logic decide whether to retry the task execution.
     */
    @Override
    public void open(Configuration configuration) throws Exception {
        ObjectResult<Properties> properties = getKafkaClientProperties();
		if(!properties.isSuccessful()) { 
			throw new RuntimeException("Failed to retrieve the Kafka Client properties could not be retrieved because " + properties.getErrorMessageCode() + " " + properties.getErrorMessage());
		}

        // --- 
        this._properties = new AtomicReference<>(properties.get());
    }

    /**
     * This method is called for each element of the input stream.
     * 
     * @param value - The input value.
     * @return The result of the map operation.
     */
    @Override
    public Properties map(Properties value) {
        return(this._properties.get());
    }
    
    /**
     * This method is called when the task is canceled or the job is stopped.
     * 
     * @throws Exception - Implementations may forward exceptions, which are
     * caught.
     */
    @Override
    public void close() throws Exception {}

    /**
     * @return the Kafka Client properties from the local properties file if no arugments are passed,
     * or from AWS Secrets Manager and AWS Systems Manager Parameter Store if --get-from-aws is passed
     * as an argument.  Otherwise, an error message occurs, an error code and message is returned.
     */
    private ObjectResult<Properties> getKafkaClientProperties() {
		if(this._useAws) {
			/*
			 * The flag was passed to the App, and therefore the properties will be fetched
			 * from AWS Systems Manager Parameter Store and Secrets Manager, respectively.
			 */            
            final KafkaClient kafkaClient = 
                new KafkaClient(
                    CONFLUENT_CLOUD_RESOURCE_PATH + this._serviceAccountUser + "/" + KAFKA_CLUSTER_SECRETS_PATH, 
                    CONFLUENT_CLOUD_RESOURCE_PATH + this._serviceAccountUser + "/" + (this._consumerKafkaClient ? KAFKA_CLIENT_CONSUMER_PARAMETERS_PATH : KAFKA_CLIENT_PRODUCER_PARAMETERS_PATH));
            return kafkaClient.getKafkaClusterPropertiesFromAws();
		} else {
			/*
			 * The flag was NOT passed to the App, therefore the properties will be fetched
			 * from a local properties file.
			 */
            try {
                Properties properties = new Properties();
                final String resourceFilename = this._consumerKafkaClient ? "consumer.properties" : "producer.properties";
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
