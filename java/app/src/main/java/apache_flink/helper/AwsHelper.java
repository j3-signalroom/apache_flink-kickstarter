/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * AWS helper functions.
 */
package apache_flink.helper;


import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.secretsmanager.*;
import com.amazonaws.services.secretsmanager.model.*;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.amazonaws.services.simplesystemsmanagement.*;
import com.amazonaws.services.simplesystemsmanagement.model.*;
import org.json.*;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.*;
import org.apache.commons.lang3.tuple.*;

import apache_flink.*;
import apache_flink.enums.*;


public class AwsHelper {
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
			awsRegion = Common.DEFAULT_AWS_REGION;
		}

		AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration("secretsmanager." + awsRegion + ".amazonaws.com", awsRegion);
		AWSSecretsManagerClientBuilder clientBuilder = AWSSecretsManagerClientBuilder.standard();
		clientBuilder.setEndpointConfiguration(config);
      	AWSSecretsManager manager = clientBuilder.build();
		GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName).withVersionStage(secretVersionId);
      	GetSecretValueResult getSecretValueResult = null;
      	try {
          	getSecretValueResult = manager.getSecretValue(getSecretValueRequest);

			// ---
			if(getSecretValueResult != null) {
				if(getSecretValueResult.getSecretString() != null) {
					return new ObjectResult<>(new JSONObject(getSecretValueResult.getSecretString()));
				} else {
					return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_REQUESTED_SECRET_DOES_HAVE_NOT_STRING.getCode(), String.format("%s does not contain a string value.", secretName));
				}
			} else {
				return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_REQUESTED_SECRET_HAS_NO_VALUE.getCode(), String.format("%s has no value.", secretName));
			}
      	} catch(ResourceNotFoundException e) {
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
	public static ObjectResult<Map<String, String>> getParameters(final String prefix){
		return getParameters(prefix, System.getenv("AWS_REGION"));
	}

	/**
	 * Get list of parameters with specified prefix.
	 *
	 * @param prefix 
	 * @param awsRegion
	 * @return The list of AWS Systems Manager parameters in the Parameter Store.
	 */
	public static ObjectResult<Map<String, String>> getParameters(final String prefix, String awsRegion){
		/*
		 * Default to 'us-east-1' if region is null
		 * 
		 * Note, setting the default because I do not have time to figure out why when locally
		 * debugging, the AWS_REGION environment variable is not being read propertly
		 */
		if(awsRegion == null) {
			awsRegion = Common.DEFAULT_AWS_REGION;
		}

		AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration("ssm." + awsRegion + ".amazonaws.com", awsRegion);
		AWSSimpleSystemsManagementClientBuilder clientBuilder = AWSSimpleSystemsManagementClientBuilder.standard();
		clientBuilder.setEndpointConfiguration(config);
		AWSSimpleSystemsManagement manager = clientBuilder.build();
		GetParametersByPathRequest getParametersByPathRequest = new GetParametersByPathRequest().withPath(prefix).withRecursive(true);

		try {
			String token = null;
			Map<String, String> params = new HashMap<>();
			do {
				getParametersByPathRequest.setNextToken(token);
				GetParametersByPathResult parameterResult = manager.getParametersByPath
						(getParametersByPathRequest);
				token = parameterResult.getNextToken();
				params.putAll(addParamsToMap(parameterResult.getParameters()));
			} while (token != null);
			return new ObjectResult<>(params);
      	} catch(ResourceNotFoundException | AWSSimpleSystemsManagementException e) {
			return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_REQUESTED_PARAMETER_PREFIX_NOT_FOUND.getCode(), e.getMessage());
      	} catch (InvalidRequestException e) {
			return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_INVALID_REQUEST.getCode(), e.getMessage());
      	} catch (InvalidParameterException e) {
			return new ObjectResult<>(ErrorEnum.ERR_CODE_AWS_INVALID_PARAMETERS.getCode(), e.getMessage());
      	}
	}

	/**
	 * 
	 * @param parameters
	 * @return
	 */
	private static Map<String,String> addParamsToMap(List<Parameter> parameters) {
        return parameters.stream().map( param -> {
			// By default assume the parameter value is a string data type
			String paramValue = param.getValue();

			/*
			 * Check if the value has zero decimal points, if so, maybe it's an integer
			 * if not, go with the default string value
			 */
			if(param.getValue().chars().filter(ch -> ch == '.').count() == 0) {
				try {
					paramValue = String.valueOf(Integer.parseInt(param.getValue().replace("," ,"")));
				} catch (Exception e) {
					// --- Ignore
				}
			} else if (param.getValue().chars().filter(ch -> ch == '.').count() == 1) {
				/*
				 * Check if the value has only one decimal point, if so, maybe it's a float
				 * if not, go with the default string value
				 */
				try {
					paramValue = String.valueOf(Float.parseFloat(param.getValue().replace("[^\\d.]", "")));
				} catch (Exception e) {
					// --- Ignore
				}
			}
           return new ImmutablePair<>(param.getName().substring(param.getName().lastIndexOf('/') + 1), paramValue);
        }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }
}
