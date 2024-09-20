from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table import StreamTableEnvironment
from pyflink.table.expressions import call, col
from pyflink.common import Types
from pyflink.table import DataTypes, TableEnvironment, EnvironmentSettings, TableSchema
from pyflink.table.udf import ScalarFunction, udf
import boto3
from botocore.exceptions import ClientError
import json
import logging
from re import sub
import os


def get_kafka_properties(kafka_cluster_secrets_path: str, kafka_client_parameters_path: str) -> tuple[str, str | None]:
    """
    This method returns the Kafka Cluster properties from the AWS Secrets Manager and Parameter Store.

    :return: An ObjectResult containing the Kafka Cluster properties collection.
    """
    properties = {}

    # Retrieve the SECRET properties from the AWS Secrets Manager
    secret = get_secrets(kafka_cluster_secrets_path)
    if secret is not None:
        try:
            # Convert the JSON object to a dictionary
            secret_data = secret
            for key in secret_data:
                properties[key] = secret_data[key]

        except json.JSONDecodeError as e:
            return None

        # Retrieve the parameters from the AWS Systems Manager Parameter Store
        parameters = get_parameters(kafka_client_parameters_path)
        if parameters is not None:
            return parameters
        else:
            return None
    else:
        return None
    
def get_secrets(secrets_name: str) -> (any):
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
    client = session.client(service_name='secretsmanager', region_name=os.environ['AWS_REGION'])
    
    logging.info("AWS_ACCESS_KEY_ID: %s", os.environ['AWS_ACCESS_KEY_ID'])
    
    try:
        get_secret_value_response = client.get_secret_value(SecretId=secrets_name)
        
        # Decrypts secret using the associated KMS (Key Management System) CMK (Customer Master Key).
        return json.loads(get_secret_value_response['SecretString'])
    except ClientError as e:
        logging.error("Failed to get secrets (%s) from the AWS Secrets Manager because of %s.", secrets_name, e)
        if e.response['Error']['Code'] == 'DecryptionFailureException' or \
            e.response['Error']['Code'] == 'InternalServiceErrorException' or \
            e.response['Error']['Code'] == 'InvalidParameterException' or \
            e.response['Error']['Code'] == 'InvalidRequestException' or \
            e.response['Error']['Code'] == 'ResourceNotFoundException':
                raise ValueError(e.response['Error']['Code'])
        return None

def get_parameters(parameter_path: str) -> (dict):
    """
    This method retrieves the parameteres from the System Manager Parameter Store.
    Moreover, it converts the values to the appropriate data type.
    
    Arg(s):
        parameter_path (str): The hierarchy for the parameter.  Hierarchies start
        with a forward slash (/). The hierarchy is the parameter name except the last
        part of the parameter.  For the API call to succeed, the last part of the
        parameter name can't be in the path. A parameter name hierarchy can have a
        maximum of 15 levels.
        
    Return(s):
        parameters (dict): Goes throught recursively and returns all the parameters
        within a hierarchy.
    """
    
    session = boto3.session.Session()
    client = session.client(service_name='ssm', region_name=os.environ['AWS_REGION'])
    
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

# Set up the environment
env = StreamExecutionEnvironment.get_execution_environment()
tbl_env = StreamTableEnvironment.create(env)

# Adjust resource configuration
env.set_parallelism(1)  # Set parallelism to 1 for simplicity

@udf(input_types=[DataTypes.BOOLEAN(), DataTypes.STRING()], result_type=DataTypes.STRING())
def udf_function(for_consumer: bool, service_account_user: str):
    # Get the Kafka Client properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
    for_consumer = True
    service_account_user = "tf_snowflake_user"
    secret_path_prefix = f"/confluent_cloud_resource/{service_account_user}"
    properties = get_kafka_properties(
        f"{secret_path_prefix}/kafka_cluster/java_client",
        f"{secret_path_prefix}/consumer_kafka_client" if for_consumer else f"{secret_path_prefix}/producer_kafka_client"
    )

    if properties is None:
        raise RuntimeError(f"Failed to retrieve the Kafka Client properties from '{secret_path_prefix}' secrets because {properties.get_error_message_code()}:{properties.get_error_message()}")
    else:
        # Set the class properties using thread-safe atomic operation
        return properties

# Register the UDF
tbl_env.create_temporary_function("udf_function", udf_function)

# Define the data stream
data_stream = env.from_collection(
    collection=[
        (True, 'user1'),
        (False, 'user2')
    ],
    type_info=Types.ROW([Types.BOOLEAN(), Types.STRING()])
)

# Create and register the input table
input_table = tbl_env.from_data_stream(data_stream, col("f0").alias("for_consumer"), col("f1").alias("service_account_user"))
tbl_env.create_temporary_view("input_table", input_table)

# Use the UDF to fetch secrets from AWS Secrets Manager in a query
result_table = tbl_env.sql_query("""
    SELECT service_account_user, udf_function(for_consumer, service_account_user) AS property_values FROM input_table
""")

# Print the result table schema for debugging purposes
result_table.print_schema()

# Convert the result table back to a data stream and print it
result_data_stream = tbl_env.to_append_stream(result_table, Types.ROW([Types.STRING(), Types.STRING()]))
result_data_stream.print()

# Execute the Flink job
env.execute("UDF Play")
