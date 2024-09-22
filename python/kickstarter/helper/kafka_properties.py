import json
from typing import Iterator
from pyflink.table.expressions import col
from pyflink.common import Row
from pyflink.table import DataTypes
from pyflink.table.udf import udtf

from helper.aws_services import AwsServices

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


_gbl_for_consumer = True
_gbl_service_account_user = "tf_snowflake_user"


def get_kafka_properties_from_aws(cluster_secrets_path: str, client_parameters_path: str) -> tuple[str, str | None]:
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

@udtf(result_types=DataTypes.ROW([DataTypes.FIELD('property_key',DataTypes.STRING()),
                                    DataTypes.FIELD('property_value', DataTypes.STRING())]))
def kafka_properties_udtf(kakfa_properties: Row) -> Iterator[Row]:
    # Get the Kafka Client properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
    for_consumer = _gbl_for_consumer
    service_account_user = _gbl_service_account_user
    secret_path_prefix = f"/confluent_cloud_resource/{service_account_user}"
    properties = get_kafka_properties_from_aws(
        f"{secret_path_prefix}/kafka_cluster/java_client",
        f"{secret_path_prefix}/consumer_kafka_client" if for_consumer else f"{secret_path_prefix}/producer_kafka_client"
    )
    if properties is None:
        raise RuntimeError(f"Failed to retrieve the Kafka Client properties from '{secret_path_prefix}' secrets because {properties.get_error_message_code()}:{properties.get_error_message()}")
    else:
        for property_key, property_value in properties.items():
            yield Row(str(property_key), str(property_value))

def get_kafka_properties(tbl_env, for_consumer: bool, service_account_user: str) -> dict[str, str]:
    global _gbl_for_consumer, _gbl_service_account_user
    _gbl_for_consumer = for_consumer
    _gbl_service_account_user = service_account_user

    # Define the schema for the table
    schema = DataTypes.ROW([
        DataTypes.FIELD('property_key', DataTypes.STRING()),
        DataTypes.FIELD('property_value', DataTypes.STRING())
    ])

    # Create an empty table
    kafka_property_table = tbl_env.from_elements([('','')], schema)

    # Register the table as a temporary view
    tbl_env.create_temporary_view('kafka_property_table', kafka_property_table)

    kafka_property_table = tbl_env.from_path('kafka_property_table')

    print('\n Kafka Property Table Schema:--->')
    kafka_property_table.print_schema()

    func_results = kafka_property_table.join_lateral(kafka_properties_udtf.alias("key", "value")).select(col("key"), col("value"))
    print('\n Kafka Property Table Data:--->')
    func_results.execute().print()

    # Convert the result into a Python dictionary
    result = func_results.execute().collect()
    result_dict = {}
    for row in result:
        if ".ms" in row[0]:
            # Convert the string representation of milliseconds into its float-point value
            result_dict[row[0]] = int(row[1])
        else:
            result_dict[row[0]] = row[1]

    result.close()

    # Return the table results into a dictionary
    print(result_dict)
    return result_dict
