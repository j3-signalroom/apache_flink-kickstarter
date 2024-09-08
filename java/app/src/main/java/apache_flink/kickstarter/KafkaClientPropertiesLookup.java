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
import java.util.*;

import apache_flink.kickstarter.helper.*;


/**
 * This class creates a Custom Source Data Stream to read the AWS Secrets Manager secrets 
 * and AWS Systems Manager Parameter Store properties during the initial set up of the 
 * Flink App, then caches the properties for use by any subsequent events that need these
 * properties.
 */
public class KafkaClientPropertiesLookup extends RichMapFunction<Properties, Properties>{
    private transient AtomicReference<Properties> _properties;
    private volatile boolean _consumerKafkaClient;
    private volatile String _serviceAccountUser;


    /**
     * Default constructor.
     * 
     * @param consumerKafkaClient
     * @param serviceAccountUser
     * @throws Exception - Exception occurs when the service account user is empty.
     */
    public KafkaClientPropertiesLookup(final boolean consumerKafkaClient, final String serviceAccountUser) throws Exception {
        // --- Check if the service account user is empty
        if(serviceAccountUser.isEmpty()) {
            throw new Exception("The service account user must be provided.");
        }

        // ---  Set the class properties
        this._consumerKafkaClient = consumerKafkaClient;
        this._serviceAccountUser = serviceAccountUser;
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
        /* 
         * Get the Kafka Client properties from AWS Secrets Manager and AWS Systems
         * Manager Parameter Store.
         */
        final String secretPathPrefix = "/confluent_cloud_resource/" + this._serviceAccountUser;
        final KafkaClient kafkaClient = 
            new KafkaClient(
                secretPathPrefix + "/kafka_cluster/java_client", 
                secretPathPrefix + (this._consumerKafkaClient ? "/consumer_kafka_client" : "/producer_kafka_client"));
        ObjectResult<Properties> properties = kafkaClient.getKafkaClusterPropertiesFromAws();

		if(!properties.isSuccessful()) { 
			throw new RuntimeException("Failed to retrieve the Kafka Client properties could not be retrieved because " + properties.getErrorMessageCode() + " " + properties.getErrorMessage());
		} else {
            // ---  Set the class properties
            this._properties = new AtomicReference<>(properties.get());
        }
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
}
