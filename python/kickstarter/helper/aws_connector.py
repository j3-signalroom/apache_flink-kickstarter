import boto3  # Import the boto3 library
from botocore.exceptions import ClientError
import json
import logging
from re import sub
from decimal import Decimal
import os


class AwsService:
    """AWS Services class, focused on the Secrets Manager, S3 and System Manager Parameter Store."""
    
    def __init__(self):
        """
        The initializor reads the AWS Region from the AWS_REGION environment variable.
        """
        
        self._aws_region_name = os.environ['AWS_REGION']
        
    def get_secrets(self, secrets_name: str) -> (any):
        """
        This method retrieve secrets from the AWS Secrets Manager.
        
        Arg(s):
            `secrets_name` (string): Pass the name of the secrets you want the secrets for.
            
        Return(s):
            If successful, returns a JSON object of the secrets' value(s) stored.  Otherwise,
            the method has failed and 'None' is returned.
            
        Raise(s):
            `DecryptionFailureException`: Secrets Manager can't decrypt the protected secret text
            using the provided KMS key.
            `InternalServiceErrorException`: An internal server error exception object.
            `InvalidParameterException`: An input parameter violated a constraint.
            `InvalidRequestException`: Indicates that something is wrong with the input to the request.
            `ResourceNotFoundExceptionAttributeError`: The operation tried to access a keyspace or table
            that doesn't exist. The resource might not be specified correctly, or its status might not
            be ACTIVE.
        """
        
        # Create a Secrets Manager client
        session = boto3.session.Session()
        client = session.client(service_name='secretsmanager', region_name=self._aws_region_name)
        
        logging.info("AWS_ACCESS_KEY_ID: %s", os.environ['AWS_ACCESS_KEY_ID'])
        
        try:
            get_secret_value_response = client.get_secret_value(SecretId=secrets_name)
            
            # Decrypts secret using the associated KMS (Key Management System) CMK (Customer Master Key).
            return json.loads(get_secret_value_response['SecretString'])
        except ClientError as e:
            logging.error("Failed to get secrets from the AWS Secrets Manager because of %s.", e)
            if e.response['Error']['Code'] == 'DecryptionFailureException' or \
               e.response['Error']['Code'] == 'InternalServiceErrorException' or \
               e.response['Error']['Code'] == 'InvalidParameterException' or \
               e.response['Error']['Code'] == 'InvalidRequestException' or \
               e.response['Error']['Code'] == 'ResourceNotFoundException':
                   raise ValueError(e.response['Error']['Code'])
            return None

    def get_parameter_values(self, parameter_path: str) -> (dict):
        """
        This method retrieves the parameteres from the System Manager Parameter Store.
        Moreover, it converts the values to the appropriate data type.
        
        Arg(s):
            parameter_path (string): The hierarchy for the parameter.  Hierarchies start
            with a forward slash (/). The hierarchy is the parameter name except the last
            part of the parameter.  For the API call to succeed, the last part of the
            parameter name can't be in the path. A parameter name hierarchy can have a
            maximum of 15 levels.  Here is an example of a hierarchy: 
            /shared/snowflake/database/vdp_acertus_dw_dev/ch
            
        Return(s):
            parameters (dictionary): Goes throught recursively and returns all the parameters
            within a hierarchy.
        """
        
        session = boto3.session.Session()
        client = session.client(service_name='ssm', region_name=self._aws_region_name)
        
        try:
            response = client.get_parameters_by_path(Path=parameter_path, Recursive=False, WithDecryption=True)
        except ClientError as e:
            logging.error("Failed to get parameters from the AWS Systems Manager Parameter Store because of %s.", e)
            raise ValueError(e.response['Error']['Code'])
        else:
            parameters = {}
            for parameter in response['Parameters']:
                # Get the value of the parameter that will constitutes the key for the dictionary
                key = parameter['Name'][parameter['Name'].rfind('/') + 1:]
                
                # By default assume the parameter value is a string data type
                value = "" + parameter['Value'] + ""
                
                # Check if the value has zero decimal points, if so, maybe it's an integer
                # if not, go with the default string value
                if parameter['Value'].count('.') == 0:
                    try:
                        value = int(parameter['Value'].replace(',',''))
                    except Exception:
                        pass
                # Check if the value has only one decimal point, if so, maybe it's a float
                # if not, go with the default string value
                elif parameter['Value'].count('.') == 1:
                    try:
                        value = float(sub(r'[^\d.]', '', parameter['Value']))
                    except Exception:
                        pass
                    
                parameters[key] = value
                
            return parameters
        