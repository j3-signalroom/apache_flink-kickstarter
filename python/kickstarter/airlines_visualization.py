import streamlit as st
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table import TableEnvironment, EnvironmentSettings, StreamTableEnvironment
from pyflink.table.catalog import ObjectPath
import argparse
import pandas as pd

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
    # --- Create a blank Flink execution environment
    env = StreamExecutionEnvironment.get_execution_environment()

    # --- Enable checkpointing every 5000 milliseconds (5 seconds)
    env.enable_checkpointing(5000)

    #
    # Set timeout to 60 seconds
    # The maximum amount of time a checkpoint attempt can take before being discarded.
    #
    env.get_checkpoint_config().set_checkpoint_timeout(60000)

    #
    # Set the maximum number of concurrent checkpoints to 1 (i.e., only one checkpoint
    # is created at a time)
    #
    env.get_checkpoint_config().set_max_concurrent_checkpoints(1)

    # Create a Table Environment
    tbl_env = StreamTableEnvironment.create(stream_execution_environment=env, environment_settings=EnvironmentSettings.new_instance().in_streaming_mode().build())

    # Create the Apache Iceberg catalog with integration with AWS Glue back by AWS S3
    catalog_name = "apache_kickstarter"
    bucket_name = args.s3_bucket_name.replace("_", "-") # To follow S3 bucket naming convention, replace underscores with hyphens if exist
    try:
        if not catalog_exist(tbl_env, catalog_name):
            tbl_env.execute_sql(f"""
                CREATE CATALOG {catalog_name} WITH (
                    'type' = 'iceberg',
                    'warehouse' = 's3://{bucket_name}/warehouse',
                    'catalog-impl' = 'org.apache.iceberg.aws.glue.GlueCatalog',
                    'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO',
                    'glue.skip-archive' = 'True',
                    'glue.region' = '{args.aws_region}'
                    );
            """)
        else:
            print(f"The {catalog_name} catalog already exists.")
    except Exception as e:
        print(f"A critical error occurred to during the processing of the catalog because {e}")
        exit(1)

    # Use the Iceberg catalog
    tbl_env.use_catalog(catalog_name)

    # Access the Iceberg catalog to query the airlines database
    catalog = tbl_env.get_catalog(catalog_name)

    # Print the current catalog name
    print(f"Current catalog: {tbl_env.get_current_catalog()}")

    # Check if the database exists.  If not, create it
    database_name = "airlines"
    try:
        if not catalog.database_exists(database_name):
            tbl_env.execute_sql(f"CREATE DATABASE IF NOT EXISTS {database_name};")
        else:
            print(f"The {database_name} database already exists.")
        tbl_env.use_database(database_name)
    except Exception as e:
        print(f"A critical error occurred to during the processing of the database because {e}")
        exit(1)

    # Print the current database name
    print(f"Current database: {tbl_env.get_current_database()}")

    # Read data from the Iceberg table
    result_table = tbl_env.sql_query(f"""
        SELECT * FROM {database_name}.flight
    """)

    df = result_table.to_pandas()
    
    st.dataframe(df)




if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--aws-s3-bucket',
                        dest='s3_bucket_name',
                        required=True,
                        help='The AWS S3 bucket name.')
    parser.add_argument('--aws-region',
                        dest='aws_region',
                        required=True,
                        help='The AWS Rgion name.')
    known_args, _ = parser.parse_known_args()
    main(known_args)
