import streamlit as st
from pyflink.common import WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, DataStream
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer, DeliveryGuarantee
from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.table import TableEnvironment, EnvironmentSettings, StreamTableEnvironment
from pyflink.table.table_schema import TableSchema
from pyflink.table.catalog import ObjectPath
from pyflink.table.expressions import *
from datetime import datetime, timezone
import argparse
import pandas as pd

from model.flight_data import FlightData
from helper.utilities import catalog_exist

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def main(args):
    """This function reads data from an Iceberg table and displays it in Streamlit.

    Args:
        args (str): is the arguments passed to the script.
    """
    env = StreamExecutionEnvironment.get_execution_environment()
    env.enable_checkpointing(5000)
    env.get_checkpoint_config().set_checkpoint_timeout(60000)
    env.get_checkpoint_config().set_max_concurrent_checkpoints(1)

    tbl_env = StreamTableEnvironment.create(env, environment_settings=EnvironmentSettings.new_instance().in_streaming_mode().build())


    # Define the CREATE CATALOG Flink SQL statement to register the Iceberg catalog
    # using the HadoopCatalog to store metadata in AWS S3 (i.e., s3a://), a Hadoop- 
    # compatible filesystem.  Then execute the Flink SQL statement to register the
    # Iceberg catalog
    catalog_name = "apache_kickstarter"
    bucket_name = args.s3_bucket_name.replace("_", "-") # To follow S3 bucket naming convention, replace underscores with hyphens if exist
    try:
        if not catalog_exist(tbl_env, catalog_name):
            tbl_env.execute_sql(f"""
                CREATE CATALOG {catalog_name} WITH (
                    'type' = 'iceberg',
                    'catalog-type' = 'in-memory',            
                    'warehouse' = 's3a://{bucket_name}/warehouse'
                    );
            """)
        else:
            print(f"The {catalog_name} catalog already exists.")
    except Exception as e:
        print(f"A critical error occurred to during the processing of the catalog because {e}")
        exit(1)

    # Use the Iceberg catalog
    tbl_env.use_catalog(catalog_name)

    # Access the Iceberg catalog to create the airlines database and the Iceberg tables
    catalog = tbl_env.get_catalog(catalog_name)

    # Print the current catalog name
    print(f"Current catalog: {tbl_env.get_current_catalog()}")
    print(tbl_env.list_catalogs())

    # Check if the database exists.  If not, create it
    database_name = "airlines"
    tbl_env.use_database(database_name)
    

    # Print the current database name
    print(f"Current database: {tbl_env.get_current_database()}")
    print(tbl_env.list_databases())

    flight_table_path = ObjectPath(database_name, "skyone_airline")

    print(f"Flight table path: {flight_table_path}")
    print(tbl_env.list_tables())

    print(catalog.get_table(flight_table_path).get_detailed_description())
    print(catalog.get_table_statistics(flight_table_path).get_row_count())
    print(catalog.get_table_statistics(flight_table_path).get_field_count())



if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--aws-s3-bucket',
                        dest='s3_bucket_name',
                        required=True,
                        help='The AWS S3 bucket name.')
    known_args, _ = parser.parse_known_args()
    main(known_args)
