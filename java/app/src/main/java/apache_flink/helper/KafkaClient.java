/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.helper;


import java.util.*;
import org.json.*;

import apache_flink.enums.*;


public class KafkaClient {
	private String kafkaClusterSecretsPath;
	private String kafkaClientParametersPath;


	/**
	 * The default constructor stores the parameter values that are passed to it.
	 * 
	 * @param kafkaClusterSecretsPath
     * @param kafkaClientParametersPath
	 */
	public KafkaClient(final String kafkaClusterSecretsPath, final String kafkaClientParametersPath) {
		this.kafkaClusterSecretsPath = kafkaClusterSecretsPath;
        this.kafkaClientParametersPath = kafkaClientParametersPath;
	}

	/**
	 * This method returns the Kafka Cluster properties from the AWS Secrets Manager and 
	 * Parameter Store.
	 *
	 * @return The Kafka Cluster properties collection.
	 */
	public ObjectResult<Properties> getKafkaClusterPropertiesFromAws() {
		Properties properties = new Properties();
		
		// --- Retrieve the SECRET properties from the AWS Secrets Manager		
		ObjectResult<JSONObject> secret = AwsHelper.getSecrets(this.kafkaClusterSecretsPath, "AWSCURRENT");
		if(secret.isSuccessful()) {
			try {
				Iterator<?> iterator = secret.get().keys();
				while (iterator.hasNext()) {
					Object key = iterator.next();
					Object value = secret.get().get(key.toString());
					properties.put(key, value);
				}
			} catch (final JSONException e) {
				return new ObjectResult<>(ErrorEnum.ERR_CODE_MISSING_OR_INVALID_FIELD.getCode(), e.getMessage());
			}

			ObjectResult<Properties> parameters = AwsHelper.getParameters(this.kafkaClientParametersPath);
			if(parameters.isSuccessful()) {
				Properties merged = new Properties();
				merged.putAll(properties);
				merged.putAll(parameters.get());
				return new ObjectResult<>(merged);
			} else {
				return new ObjectResult<>(parameters.getErrorMessageCode(), parameters.getErrorMessage());	
			}
		} else {
			return new ObjectResult<>(secret.getErrorMessageCode(), secret.getErrorMessage());
		}
	}
}
