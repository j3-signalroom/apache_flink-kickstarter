/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.kickstarter.datastream_api;

import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import apache_flink.*;
import apache_flink.helper.*;

import org.apache.flink.configuration.Configuration;
import java.util.*;

public class KafkaClientPropertiesSource extends RichSourceFunction<Properties>{
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
        ObjectResult<Properties> properties = Common.getKafkaClientProperties(this.consumerKafkaClient, this.args);
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
}
