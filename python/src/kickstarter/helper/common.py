from typing import Dict, Tuple
from pyflink.datastream import StreamExecutionEnvironment
from datetime import datetime, timezone
from pyflink.table.catalog import Catalog

from aws_clients_python_lib.secrets_manager import get_secrets
from aws_clients_python_lib.parameter_store import get_parameters


__copyright__  = "Copyright (c) 2024-2025 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def serialize_date(obj):
    """
    This method serializes the `obj` parameter to a string.

    Args:
        obj (obj):  The object to serialize, if type datetime or date.

    Returns:
        str:  If the obj is of type datetime or date, the object is formatted 
        according to ISO 8601 (i.e., 'YYYY-MM-DD HH:MM:SS').  Otherwise, 
        the obj is returned as is.
    """
    if isinstance(obj, str):
        return obj
    return obj.isoformat(timespec="milliseconds")


def parse_isoformat(date_string: str) -> datetime:
    """This method parses a string representing a date and time in ISO 8601 format.

    Args:
        date_string (str): The string representing a date and time in ISO 8601 format.

    Returns:
        datetime: The datetime object representing the date and time in ISO 8601 format.
    """
    try:
        date_time_obj = datetime.strptime(date_string, "%Y-%m-%d %H:%M:%S")
        date_time_utc = date_time_obj.replace(tzinfo=timezone.utc).astimezone(timezone.utc)
        return date_time_utc
    except ValueError:
        print(f"Invalid isoformat string: '{date_string}'")
        return None
    

def catalog_exist(tbl_env: StreamExecutionEnvironment, catalog_to_check: str) -> bool:
    """This method checks if the catalog exist in the environment.

    Args:
        tbl_env (StreamExecutionEnvironment): The StreamExecutionEnvironment is the context
        in which a streaming program is executed. 
        catalog_to_check (str): The name of the catalog to be checked if its name exist in the
        environment.

    Returns:
        bool: True is returned, if the catalog exist in the environment.  Otherwise, False is
        returned.
    """
    catalogs = tbl_env.list_catalogs()

    # Check if a specific catalog exists
    if catalog_to_check in catalogs:
        return True
    else:
        return False
    

def load_catalog(tbl_env: StreamExecutionEnvironment, region_name: str, bucket_name: str, catalog_name: str) -> Catalog:
    """ This method loads the catalog into the environment.
    
    Args:
        tbl_env (StreamExecutionEnvironment): The StreamExecutionEnvironment is the context
        in which a streaming program is executed. 
        region_name (str): The region where the bucket is located.
        bucket_name (str): The name of the bucket where the warehouse is located.
        catalog_name (str): The name of the catalog to be loaded into the environment.
        
    Returns:
        Catalog: The catalog object is returned if the catalog is loaded into the environment.
    """
    try:
        if not catalog_exist(tbl_env, catalog_name):
            tbl_env.execute_sql(f"""
                CREATE CATALOG {catalog_name} WITH (
                    'type' = 'iceberg',
                    'warehouse' = '{bucket_name}',
                    'catalog-impl' = 'org.apache.iceberg.aws.glue.GlueCatalog',
                    'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO',
                    'glue.skip-archive' = 'True',
                    'glue.region' = '{region_name}'
                    );
            """)
        else:
            print(f"The {catalog_name} catalog already exists.")
    except Exception as e:
        print(f"A critical error occurred to during the processing of the catalog because {e}")
        exit(1)

    # --- Use the Iceberg catalog
    tbl_env.use_catalog(catalog_name)

    # --- Access the Iceberg catalog to query the airlines database
    return tbl_env.get_catalog(catalog_name)


def load_database(tbl_env: StreamExecutionEnvironment, catalog: Catalog, database_name:str) -> None:
    """This method loads the database into the environment.

    Args:
        tbl_env (StreamExecutionEnvironment): The StreamExecutionEnvironment is the con.text
        catalog (Catalog): The catalog object is the catalog to be used to create the database.
        database_name (str): The name of the database to be loaded into the environment.
    """
    try:
        if not catalog.database_exists(database_name):
            tbl_env.execute_sql(f"CREATE DATABASE IF NOT EXISTS {database_name};")
        else:
            print(f"The {database_name} database already exists.")
        tbl_env.use_database(database_name)
    except Exception as e:
        print(f"A critical error occurred to during the processing of the database because {e}")
        exit(1)


def get_confluent_properties(aws_region_name: str, kafka_cluster_secrets_path: str, client_parameters_path: str) -> Tuple[Dict[str, str], str]:
    """This method retrieves the Kafka Cluster properties from the AWS Secrets Manager, and the Kafka Client 
    properties from the AWS Systems Manager.

    Args:
        aws_region_name (str): is the AWS region name where the AWS Secrets Manager and
        AWS Systems Manager Parameter Store are located.
        kafka_cluster_secrets_path (str): the path to the Kafka Cluster secrets in the AWS Secrets Manager.
        client_parameters_path (str): the path to the Kafka Client parameters in the AWS Systems Manager
        Parameter Store.

    Returns:
        Tuple[Dict[str, str], str]: the Kafka Cluster properties collection if successful, otherwise None.
    """
    confluent_properties = {}
    
    # Retrieve the Kafka Cluster properties from the AWS Secrets Manager
    kafka_cluster_properties, error_message = get_secrets(aws_region_name, kafka_cluster_secrets_path)
    if error_message:
        return {}, error_message
    confluent_properties.update(kafka_cluster_properties)

    # Retrieve the parameters from the AWS Systems Manager Parameter Store
    parameters, error_message = get_parameters(aws_region_name, client_parameters_path)
    if error_message:
        return {}, error_message
    confluent_properties.update(parameters)

    return confluent_properties, ""