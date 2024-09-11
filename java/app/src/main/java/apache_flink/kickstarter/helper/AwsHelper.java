/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * AWS helper functions.
 */
package apache_flink.kickstarter.helper;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.*;
import software.amazon.awssdk.services.secretsmanager.model.*;
import software.amazon.awssdk.services.ssm.*;
import software.amazon.awssdk.services.ssm.model.*;
import org.json.*;

import apache_flink.kickstarter.enums.*;

import java.security.InvalidParameterException;
import java.util.*;


public class AwsHelper {
	public static final String DEFAULT_AWS_REGION = "us-east-1";

	private AwsHelper() {}

	/**
	 * Get the the secret values from AWS.
	 *
	 * @param secretName
	 * @param secretVersionId
	 * @return
	 */
	public static ObjectResult<JSONObject> getSecrets(final String secretName, final String secretVersionId) {
		return getSecrets(secretName, secretVersionId, System.getenv("AWS_REGION"));
	}

	/**
	 * Get the the secret values from AWS.
	 *
	 * @param secretName
	 * @param secretVersionId
	 * @param awsRegion
	 * @return
	 */
	public static ObjectResult<JSONObject> getSecrets(final String secretName, final String secretVersionId, String awsRegion) {
		/*
		 * Default to 'us-east-1' if region is null
		 * 
		 * Note, setting the default because I do not have time to figure out why when locally
		 * debugging, the AWS_REGION environment variable is not being read propertly
		 */
		if(awsRegion == null) {
			awsRegion = DEFAULT_AWS_REGION;
		}
		Region region = Region.of(awsRegion);

		SecretsManagerClient client = 
			SecretsManagerClient.builder()
				.region(region)
				.build();

		GetSecretValueRequest getSecretValueRequest = 
			GetSecretValueRequest.builder()
				.secretId(secretName)
				.build();

		GetSecretValueResponse getSecretValueResponse;
      	try {
			getSecretValueResponse = client.getSecretValue(getSecretValueRequest);

			// ---
			if(getSecretValueResponse != null) {
				if(getSecretValueResponse.secretString() != null) {
					return new ObjectResult<>(new JSONObject(getSecretValueResponse.secretString()));
				} else {
					return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_REQUESTED_SECRET_DOES_HAVE_NOT_STRING.getCode(), String.format("%s does not contain a string value.", secretName));
				}
			} else {
				return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_REQUESTED_SECRET_HAS_NO_VALUE.getCode(), String.format("%s has no value.", secretName));
			}
      	} catch(software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException e) {
			return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_REQUESTED_SECRET_NOT_FOUND.getCode(), e.getMessage());
      	} catch (InvalidRequestException e) {
			return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_INVALID_REQUEST.getCode(), e.getMessage());
      	} catch (InvalidParameterException e) {
			return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_INVALID_PARAMETERS.getCode(), e.getMessage());
      	}
	}

	/**
	 * Get list of parameters with specified prefix.
	 *
	 * @param prefix 
	 * @return The list of AWS Systems Manager parameters in the Parameter Store.
	 */
	public static ObjectResult<Properties> getParameters(final String prefix){
		return getParameters(prefix, System.getenv("AWS_REGION"));
	}

	/**
	 * Get list of parameters with specified prefix.
	 *
	 * @param prefix 
	 * @param awsRegion
	 * @return The list of AWS Systems Manager parameters in the Parameter Store.
	 */
	public static ObjectResult<Properties> getParameters(final String prefix, String awsRegion){
		Properties properties = new Properties();
		/*
		 * Default to 'us-east-1' if region is null
		 * 
		 * Note, setting the default because I do not have time to figure out why when locally
		 * debugging, the AWS_REGION environment variable is not being read propertly
		 */
		if(awsRegion == null) {
			awsRegion = DEFAULT_AWS_REGION;
		}

		Region region = Region.of(awsRegion);
		SsmClient ssmClient = 
			SsmClient.builder()
				.region(region)
				.build();

		GetParametersByPathRequest getParametersByPathRequest = 
			GetParametersByPathRequest.builder()
				.path(prefix)
				.recursive(true)
				.withDecryption(true)
				.build();

		GetParametersByPathResponse response = ssmClient.getParametersByPath(getParametersByPathRequest);

        // Process the response
        for (Parameter parameter : response.parameters()) {
			// By default assume the parameter value is a string data type
			String paramValue = parameter.value();

			/*
			 * Check if the value has zero decimal points, if so, maybe it's an integer
			 * if not, go with the default string value
			 */
			if(parameter.value().chars().filter(ch -> ch == '.').count() == 0) {
				try {
					paramValue = String.valueOf(Integer.parseInt(parameter.value().replace("," ,"")));
				} catch (Exception e) {
					// --- Ignore
				}
			} else if (parameter.value().chars().filter(ch -> ch == '.').count() == 1) {
				/*
				 * Check if the value has only one decimal point, if so, maybe it's a float
				 * if not, go with the default string value
				 */
				try {
					paramValue = String.valueOf(Float.parseFloat(parameter.value().replace("[^\\d.]", "")));
				} catch (Exception e) {
					// --- Ignore
				}
			}
			properties.setProperty(parameter.name().replace(prefix + "/", ""), paramValue);
        }

        // Close the client
        ssmClient.close();

		return new ObjectResult<>(properties);
	}
}
