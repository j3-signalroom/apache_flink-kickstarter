from typing import Iterator
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table import StreamTableEnvironment
from pyflink.table.expressions import call, col
from pyflink.common import Types, Row
from pyflink.table import DataTypes, TableEnvironment, EnvironmentSettings, Schema, TableDescriptor
from pyflink.table.udf import udtf, udf
import json
from re import sub

from aws_services import AwsServices

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def get_kafka_properties(cluster_secrets_path: str, client_parameters_path: str) -> tuple[str, str | None]:
    """This method returns the Kafka Cluster properties from the AWS Secrets Manager and Parameter Store.

    Args:
        cluster_secrets_path (str): the path to the Kafka Cluster secrets in the AWS Secrets Manager.
        client_parameters_path (str): the path to the Kafka Client parameters in the AWS Systems Manager
        Parameter Store.

    Returns:
        properties (tuple[str, str | None]): the Kafka Cluster properties collection if successful, otherwise None.
    """
    
    properties = {}

    # Retrieve the SECRET properties from the AWS Secrets Manager
    aws_services = AwsServices()
    secret = aws_services.get_secrets(cluster_secrets_path)
    if secret is not None:
        try:
            # Convert the JSON object to a dictionary
            secret_data = secret
            for key in secret_data:
                properties[key] = secret_data[key]

        except json.JSONDecodeError as e:
            return None

        # Retrieve the parameters from the AWS Systems Manager Parameter Store
        parameters = aws_services.get_parameters(client_parameters_path)
        if parameters is not None:
            for key in parameters:
                properties[key] = parameters[key]
            return properties
        else:
            return None
    else:
        return None
    


# Set up the environment
env = StreamExecutionEnvironment.get_execution_environment()
tbl_env = StreamTableEnvironment.create(env)

# Adjust resource configuration
env.set_parallelism(1)  # Set parallelism to 1 for simplicity

@udtf(result_types=DataTypes.ROW([DataTypes.FIELD('property_key',DataTypes.STRING()),
                                 DataTypes.FIELD('property_value', DataTypes.STRING())]))
def kafka_properties_udtf(kakfa_properties: Row) -> Iterator[Row]:
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
        for property_key, property_value in properties.items():
            yield Row(str(property_key), str(property_value))

# Seed table with empty data
example_data = [('','')]

# Define the schema for the table
schema = DataTypes.ROW([
    DataTypes.FIELD('property_key', DataTypes.STRING()),
    DataTypes.FIELD('property_value', DataTypes.STRING())
])

# Create a table from the empty and the provide schema
kafka_property_table = tbl_env.from_elements(example_data, schema)

# Register the table as a temporary view
tbl_env.create_temporary_view('kafka_property_table', kafka_property_table)

kafka_property_table = tbl_env.from_path('kafka_property_table')
print('\n Kafka Property Table Schema:--->')
kafka_property_table.print_schema()

print('\n Kafka Property Table Data:--->')
device_stats = kafka_property_table.join_lateral(kafka_properties_udtf.alias("key", "value")).select(col("key"), col("value"))
device_stats.execute().print()

# Convert the result into a Python dictionary
# First, collect the results using to_data_stream or execute.collect()
result = device_stats.execute().collect()

# Process the results into a Python dictionary
result_dict = {}
for row in result:
    if ".ms" in row[0]:
        """Convert the string representation of milliseconds into its float-point value"""
        result_dict[row[0]] = int(row[1])
    else:
        result_dict[row[0]] = row[1]

result.close()

# Now you have the table result in a dictionary
print(result_dict)