from pyflink.common import Row
from pyflink.table import DataTypes, StreamTableEnvironment
from pyflink.table.expressions import col
from pyflink.table.udf import udtf, TableFunction
from typing import Iterator, Dict, Tuple
import boto3
from botocore.exceptions import ClientError
import json
import os
from re import sub
import logging

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


class ConfluentProperties(TableFunction):
    """This method retrieves the Kafka Cluster and Schema Registry Cluster properties from the
    AWS Secrets Manager, and the Kafka Client properties from the AWS Systems Manager. It yields
    the combination of Kafka Cluster properties, Schema Registry Cluster properties, and Kafka
    Client parameters.
    """
    def __init__(self, is_consumer: bool, service_account_user: str):
        """Initializes the UDTF with the necessary parameters.

        Args:
            is_consumer (bool): determines if the Kafka Client is a consumer or producer.
            service_account_user (str): is the name of the service account user.  It is used in
            the prefix to the path of the Kafka Cluster secrets in the AWS Secrets Manager and
            the Kafka Client parameters in the AWS Systems Manager Parameter Store.
        """
        self._is_consumer = is_consumer
        self._service_account_user = service_account_user
        self._aws_region_name = os.environ['AWS_REGION']

    def eval(self, confluent_properties: Row) -> Iterator[Row]:
        """This method retrieves the Kafka Cluster and Schema Registry Cluster properties from the
        AWS Secrets Manager, and the Kafka Client properties from the AWS Systems Manager. It
        yields the combination of Kafka Cluster properties, Schema Registry Cluster properties,
        and Kafka Client parameters. 

        Args:
            confluent_properties (Row): is a Row object that contains the Kafka Cluster
            properties, Schema Registry Cluster properties, and Kafka Client parameters.

        Raises:
            RuntimeError: is a generic error that is raised when an error occurs.

        Yields:
            Iterator[Row]: combination of Kafka Cluster properties, Schema Registry Cluster properties,
            and Kafka Client parameters.
        """
        # Get the Kafka Client/Schema Registry Registry properties from AWS Secrets Manager and AWS
        # Systems Manager Parameter Store.
        secret_path_prefix = f"/confluent_cloud_resource/{self._service_account_user}"

        properties = self.__get_confluent_properties(
            # Using the `java_clent`` configuration instead of the `python_client`` configuration 
            # because PyFlink converts into Java code and the Java code is what is executed.
            f"{secret_path_prefix}/kafka_cluster/java_client",
            f"{secret_path_prefix}/schema_registry_cluster/java_client",
            f"{secret_path_prefix}/consumer_kafka_client" if self._is_consumer else f"{secret_path_prefix}/producer_kafka_client"
        )
        if properties is None:
            raise RuntimeError(f"Failed to retrieve the Kafka Client properties from '{secret_path_prefix}' secrets because {properties.get_error_message_code()}:{properties.get_error_message()}")
        else:
            for property_key, property_value in properties.items():
                yield Row(str(property_key), str(property_value))

    def __get_confluent_properties(self, kafka_cluster_secrets_path: str, schema_registry_cluster_secrets_path: str, client_parameters_path: str) -> tuple[str, str]:
        """This method retrieves the Kafka Cluster and Schema Registry Cluster properties from the
        AWS Secrets Manager, and the Kafka Client properties from the AWS Systems Manager.

        Args:
            kafka_cluster_secrets_path (str): the path to the Kafka Cluster secrets in the AWS Secrets Manager.
            schema_registry_cluster_secrets_path (str): the path to the Schema Registry Cluster secrets in the AWS
            Secrets Manager.
            client_parameters_path (str): the path to the Kafka Client parameters in the AWS Systems Manager
            Parameter Store.

        Returns:
            properties (tuple[str, str]): the Kafka Cluster properties collection if successful, otherwise None.
        """
        properties = {}

        # Retrieve the Kafka Cluster properties from the AWS Secrets Manager
        secret = self.__get_secrets(kafka_cluster_secrets_path)
        if secret is not None:
            try:
                # Convert the JSON object to a dictionary
                secret_data = secret
                for key in secret_data:
                    properties[key] = secret_data[key]

            except json.JSONDecodeError:
                return None
        else:
            return None
        
        # Retrieve the Schema Registry Cluster properties from the AWS Secrets Manager
        secret = self.__get_secrets(schema_registry_cluster_secrets_path)
        if secret is not None:
            try:
                # Convert the JSON object to a dictionary
                secret_data = secret
                for key in secret_data:
                    properties[key] = secret_data[key]

            except json.JSONDecodeError:
                return None
        else:
            return None

        # Retrieve the parameters from the AWS Systems Manager Parameter Store
        parameters = self.__get_parameters(client_parameters_path)
        if parameters is not None:
            for key in parameters:
                properties[key] = parameters[key]
            return properties
        else:
            return None
        
        
    def __get_secrets(self, secrets_name: str) -> (dict):
        """This method retrieve secrets from the AWS Secrets Manager.
        
        Arg(s):
            secrets_name (str): Pass the name of the secrets you want the secrets for.
            
        Return(s):
            If successful, returns a JSON object of the secrets' value(s) stored.  Otherwise,
            the method has failed and 'None' is returned.
            
        Raise(s):
            DecryptionFailureException: Secrets Manager can't decrypt the protected secret text
            using the provided KMS key.
            InternalServiceErrorException: An internal server error exception object.
            InvalidParameterException: An input parameter violated a constraint.
            InvalidRequestException: Indicates that something is wrong with the input to the request.
            ResourceNotFoundExceptionAttributeError: The operation tried to access a keyspace or table
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
            logging.error("Failed to get secrets (%s) from the AWS Secrets Manager because of %s.", secrets_name, e)
            if e.response['Error']['Code'] == 'DecryptionFailureException' or \
                e.response['Error']['Code'] == 'InternalServiceErrorException' or \
                e.response['Error']['Code'] == 'InvalidParameterException' or \
                e.response['Error']['Code'] == 'InvalidRequestException' or \
                e.response['Error']['Code'] == 'ResourceNotFoundException':
                    raise ValueError(e.response['Error']['Code'])
            return None

    def __get_parameters(self, parameter_path: str) -> (dict):
        """This method retrieves the parameteres from the System Manager Parameter Store.
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


def execute_confluent_properties_udtf(tbl_env: StreamTableEnvironment, is_consumer: bool, service_account_user: str) -> Tuple[Dict[str, str], Dict[str, str]]:
    """This method retrieves the Kafka Cluster and Schema Registry Cluster properties from the
    AWS Secrets Manager, and the Kafka Client properties from the AWS Systems Manager.

    Args:
        tbl_env (TableEnvironment): is the instantiated Flink Table Environment.  A Table
        Environment is a unified entry point for creating Table and SQL API programs.  It
        is used to convert a DataStream into a Table or vice versa.  It is also used to
        register a Table in a catalog.
        is_consumer (bool): determines if the Kafka Client is a consumer or producer.
        service_account_user (str): is the name of the service account user.  It is used in
        the prefix to the path of the Kafka Cluster and Schema Registry Cluster secrets in 
        the AWS Secrets Manager, and the Kafka Client parameters in the AWS Systems Manager 
        Parameter Store.

    Returns:
        Tuple[Dict[str, str], Dict[str, str]]: combination of Kafka Cluster and Kafka Client
        properties and Schema Registry Cluster properties.
    """
    # Define the schema for the table and the return result of the UDTF
    schema = DataTypes.ROW([
        DataTypes.FIELD('property_key', DataTypes.STRING()),
        DataTypes.FIELD('property_value', DataTypes.STRING())
    ])

    # Create an empty table
    confluent_property_table = tbl_env.from_elements([('','')], schema)

    # Define the table name based on the type of Kafka client
    table_name = "confluent_property_table_" + ("consumer" if is_consumer else "producer")

    # Register the table as a temporary view
    tbl_env.create_temporary_view(table_name, confluent_property_table)

    # Get the table from the temporary view
    confluent_property_table = tbl_env.from_path(table_name)

    # print('\n Confluent Property Table Schema:--->')
    # confluent_property_table.print_schema()

    # Register the Python function as a PyFlink UDTF (User-Defined Table Function)
    kafka_properties_udtf = udtf(f=ConfluentProperties(is_consumer, service_account_user), 
                                 result_types=schema)

    # Join the Confluent Property Table with the UDTF
    func_results = confluent_property_table.join_lateral(kafka_properties_udtf.alias("key", "value")).select(col("key"), col("value"))

    # print("\n Confluent " + ("Consumer" if is_consumer else "Producer") + " Client Property Table Data:--->")
    # func_results.execute().print()

    # Convert results using dictionary comprehension that generates a Schema Registry 
    # Cluster properties dict
    kafka_dict = {row[0]: row[1] for row in func_results.execute().collect()}
    func_results.execute().collect().close()

    # Converts the table into a datastream
    # result_stream = tbl_env.to_data_stream(confluent_property_table)
    
    # print('\n Kafka Cluster and Client Properties Python dictionary:--->')
    # print(kafka_dict)

    # Dictionary Comprehension that generates a Schema Registry Cluster properties dict
    schema_registry_dict = { key: value for key, value in kafka_dict.items() if key.startswith("schema.registry.") }

    # print('\n Schema Registry Cluster Properties Python dictionary:--->')
    # print(schema_registry_dict)

    # Returns the Kafka Cluster properties and Schema Registry Cluster properties
    return kafka_dict, schema_registry_dict
