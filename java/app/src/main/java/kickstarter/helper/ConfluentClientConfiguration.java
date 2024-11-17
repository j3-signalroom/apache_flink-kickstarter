/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.helper;


import java.util.*;
import org.json.*;

import kickstarter.enums.*;


public class ConfluentClientConfiguration {
	private String kafkaClusterSecretsPath;
	private String schemaRegistryClusterSecretsPath;
	private String kafkaClientParametersPath;


	/**
	 * The default constructor stores the parameter values that are passed to it.
	 * 
	 * @param kafkaClusterSecretsPath
	 * @param schemaRegistryClusterSecretsPath
     * @param kafkaClientParametersPath
	 */
	public ConfluentClientConfiguration(final String kafkaClusterSecretsPath, final String schemaRegistryClusterSecretsPath, final String kafkaClientParametersPath) {
		this.kafkaClusterSecretsPath = kafkaClusterSecretsPath;
		this.schemaRegistryClusterSecretsPath = schemaRegistryClusterSecretsPath;
        this.kafkaClientParametersPath = kafkaClientParametersPath;
	}

	/**
	 * This method retrieves from the AWS Secrets Manager and Parameter Store the Kafka Cluster, Schema
	 * Registry Cluster, and specified client configuration properties.
	 *
	 * @return combination of the Kafka Cluster, Schema Registry Cluster, and specified client configuration
	 * properties from the AWS Secrets Manager and Parameter Store.
	 */
	public ObjectResult<Properties> getConfluentPropertiesFromAws() {
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

			ObjectResult<JSONObject> scrSecrets = AwsHelper.getSecrets(this.schemaRegistryClusterSecretsPath, "AWSCURRENT");
			if(scrSecrets.isSuccessful()) {
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
				return new ObjectResult<>(scrSecrets.getErrorMessageCode(), scrSecrets.getErrorMessage());
			}
		} else {
			return new ObjectResult<>(secret.getErrorMessageCode(), secret.getErrorMessage());
		}
	}
}
