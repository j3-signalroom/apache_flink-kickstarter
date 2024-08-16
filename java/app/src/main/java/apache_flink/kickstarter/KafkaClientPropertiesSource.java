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

import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.configuration.Configuration;
import java.io.*;
import java.util.*;

import apache_flink.kickstarter.enums.*;
import apache_flink.kickstarter.helper.*;



public class KafkaClientPropertiesSource extends RichSourceFunction<Properties>{
    private static final String FLAG_GET_FROM_AWS = "--get-from-aws";
    private static final String CONFLUENT_CLOUD_RESOURCE_PATH = "/confluent_cloud_resource/";
    private static final String KAFKA_CLUSTER_SECRETS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "kafka_cluster/java_client";
    private static final String KAFKA_CLIENT_CONSUMER_PARAMETERS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "consumer_kafka_client";
    private static final String KAFKA_CLIENT_PRODUCER_PARAMETERS_PATH = CONFLUENT_CLOUD_RESOURCE_PATH + "producer_kafka_client";

    @SuppressWarnings("unused")
    private volatile boolean isRunning = true;

    private volatile String[] args;
    private volatile boolean consumerKafkaClient;


    /**
     * Default constructor.  The arguments are stored for later use.
     * 
     * @param consumerKafkaClient true if the Kafka Client is a consumer, false if the Kafka Client
     * is a producer.
     * @param args list of strings passed to the main method.
     */
    public KafkaClientPropertiesSource(final boolean consumerKafkaClient, final String[] args) {
        this.consumerKafkaClient = consumerKafkaClient;
        this.args = args;        
    }

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
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
    }

    @Override
    public void run(SourceContext<Properties> ctx) throws Exception {
        ObjectResult<Properties> properties = getKafkaClientProperties(this.consumerKafkaClient, this.args);
		if(!properties.isSuccessful()) { 
			throw new RuntimeException("Failed to retrieve the Kafka Client properties could not be retrieved because " + properties.getErrorMessageCode() + " " + properties.getErrorMessage());
		}
        
        // --- Emit the properties to the downstream operators
        ctx.collect(properties.get());
        
        // --- Stop the source after emitting the data once
        isRunning = false;
    }

    /**
     * Cancels the source.   Most sources will have a while loop inside the
     * run(SourceContext) method.  The implementation needs to ensure that the 
     * source will break out of that loop after this method is called.
     * 
     * A typical pattern is to have an "volatile boolean isRunning" flag that is
     * set to false in this method. That flag is checked in the loop condition.
     * 
     * In case of an ungraceful shutdown (cancellation of the source operator, 
     * possibly for failover), the thread that calls run(SourceContext) will also
     * be interrupted) by the Flink runtime, in order to speed up the cancellation
     * (to ensure threads exit blocking methods fast, like I/O, blocking queues,
     * etc.). The interruption happens strictly after this method has been called, 
     * so any interruption handler can rely on the fact that this method has completed
     * (for example to ignore exceptions that happen after cancellation).
     * 
     * During graceful shutdown (for example stopping an app with a savepoint), the
     * program must cleanly exit the run(SourceContext) method soon after this method
     * was called.  The Flink runtime will NOT interrupt the source thread during graceful
     * shutdown. Source implementors must ensure that no thread interruption happens on
     * any thread that emits records through the SourceContext from the run(SourceContext)
     * method; otherwise the clean shutdown may fail when threads are interrupted while
     * processing the final records.  Because the SourceFunction cannot easily differentiate
     * whether the shutdown should be graceful or ungraceful, we recommend that implementors
     * refrain from interrupting any threads that interact with the SourceContext at all. 
     * You can rely on the Flink runtime to interrupt the source thread in case of ungraceful
     * cancellation.  Any additionally spawned threads that directly emit records through 
     * the SourceContext should use a shutdown method that does not rely on thread interruption.
     */
    @Override
    public void cancel() {
        isRunning = false;
    }

    /**
     * This method Loops through the `args` parameter and checks for the`FLAG_GET_FROM_AWS` flag.
     * 
     * @param args list of strings passed to the main method.
     * @return true if the flag is found, false otherwise.
     */
    private boolean checkForFlagGetFromAws(final String[] args) {
        for (String arg:args) {
			if(arg.equalsIgnoreCase(FLAG_GET_FROM_AWS))
                return true;
		}
        return false;
    }

    /**
     * This method retrieves the Kafka Client properties from either the local properties files
     * or AWS Secrets Manager and AWS Systems Manager Parameter Store.
     * 
     * @param consumerKafkaClient true if the Kafka Client is a consumer, false if the Kafka Client
     * is a producer.
     * @param args list of strings passed to the main method.
     * @return the Kafka Client properties from the local properties file if no arugments are passed,
     * or from AWS Secrets Manager and AWS Systems Manager Parameter Store if --get-from-aws is passed
     * as an argument.  Otherwise, an error message occurs, an error code and message is returned.
     */
    private ObjectResult<Properties> getKafkaClientProperties(final boolean consumerKafkaClient, final String[] args) {
		if(checkForFlagGetFromAws(args)) {
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
