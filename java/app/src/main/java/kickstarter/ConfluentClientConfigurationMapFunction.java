/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * An Apache Flink custom data source stream is a user-defined source of data that
 * is integrated into a Flink application to read and process data from non-standard
 * or custom sources. This custom source can be anything that isn't supported by Flink
 * out of the box, such as proprietary REST APIs, specialized databases, custom hardware 
 * interfaces, etc.  This code uses a Custom Data Source Stream to read the AWS Secrets 
 * Manager secrets and AWS Systems Manager Parameter Store properties during the initial
 * start of the Flink App, then caches the properties for use by any subsequent events
 * that need these properties.
 */
package kickstarter;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.functions.RichMapFunction;

import kickstarter.helper.ConfluentClientConfiguration;
import kickstarter.helper.ObjectResult;


/**
 * This class creates a Custom Source Data Stream to read the AWS Secrets Manager secrets 
 * and AWS Systems Manager Parameter Store properties during the initial set up of the 
 * Flink App, then caches the properties for use by any subsequent events that need these
 * properties.
 */
public class ConfluentClientConfigurationMapFunction extends RichMapFunction<Properties, Properties>{
    private transient AtomicReference<Properties> _properties;
    private volatile boolean _isConsumer;
    private volatile String _serviceAccountUser;


    /**
     * This constrcutor initializes the class properties.
     * 
     * @param isConsumer - A boolean value that indicates if the Kafka client is a consumer.
     * @param serviceAccountUser - The service account user name plays a role in the path name
     * that is used to construct the secrets path for the AWS Secrets Manager secrets, and AWS
     * Systems Manager Parameter Store path.
     * @throws Exception - Exception occurs when the service account user is empty.
     */
    public ConfluentClientConfigurationMapFunction(boolean isConsumer, String serviceAccountUser) throws Exception {
        // --- Check if the service account user is empty
        if(serviceAccountUser.isEmpty()) {
            throw new Exception("The service account user must be provided.");
        }

        // ---  Set the class properties
        this._isConsumer = isConsumer;
        this._serviceAccountUser = serviceAccountUser;
    }

    /**
     * This method is called once per parallel task instance when the job starts. It
     * retrieves the Kafka Cluster and Schema Registry Cluster properties from the
     * AWS Secrets Manager, and the Kafka Client properties from the AWS Systems Manager. 
     * Then the properties are stored in the class properties.
     * 
     * @param openContext - The openContext object passed to the function can be used for
     * configuration and initialization. The openContext contains some necessary information 
     * that were configured on the function in the program composition.
     *
     * @throws Exception - Implementations may forward exceptions, which are caught
     * by the runtime.  When the runtime catches an exception, it aborts the task and 
     * lets the fail-over logic decide whether to retry the task execution.
     */
    @Override
    public void open(OpenContext openContext) throws Exception {
        final String secretPathPrefix = "/confluent_cloud_resource/" + this._serviceAccountUser;
        final ConfluentClientConfiguration confluentClientConfiguration = 
            new ConfluentClientConfiguration(
                secretPathPrefix + "/kafka_cluster/java_client", 
                secretPathPrefix + "/schema_registry_cluster/java_client",
                secretPathPrefix + (this._isConsumer ? "/consumer_kafka_client" : "/producer_kafka_client"));
        ObjectResult<Properties> properties = confluentClientConfiguration.getConfluentPropertiesFromAws();

		if(!properties.isSuccessful()) { 
			throw new RuntimeException(String.format("Failed to retrieve the Kafka Client properties from '%s' secrets because %s:%s", secretPathPrefix, properties.getErrorMessageCode(), properties.getErrorMessage()));
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
     * For this particular class, it is not used.
     * 
     * @throws Exception - Implementations may forward exceptions, which are
     * caught.
     */
    @Override
    public void close() throws Exception {}
}
