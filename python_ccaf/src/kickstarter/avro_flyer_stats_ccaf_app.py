from pyflink.table import (TableEnvironment, Schema, DataTypes, FormatDescriptor)
from pyflink.table.catalog import ObjectPath
from pyflink.table.confluent import ConfluentSettings, ConfluentTableDescriptor
from pyflink.table.expressions import col, lit
import argparse

from src.kickstarter.helper.settings import get_secrets, FLINK_CLOUD, FLINK_REGION, FLINK_COMPUTE_POOL_ID, FLINK_API_KEY, FLINK_API_SECRET, ORGANIZATION_ID, ENVIRONMENT_ID 


__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def run():
    """
    The run() method is the main entry point for the application.  It sets up
    the TableEnvironment, defines the Kafka sink table, reads from source tables,
    transforms the data, and writes to the sink.

    :return: None
    """
    # The service account user is passed in as a command line argument.  
    parser = argparse.ArgumentParser()
    parser.add_argument('--service-account-user',
                        dest='service_account_user',
                        required=True,
                        help='The Service Account User.')
    parser.add_argument('--aws-region',
                        dest='aws_region',
                        required=True,
                        help='The AWS Region.')
    known_args, _ = parser.parse_known_args()
    service_account_user = known_args.service_account_user.lower()
    aws_region = known_args.aws_region.lower()

    # Retrieve the Confluent Cloud settings from AWS Secrets Manager.
    secret_name = f"/confluent_cloud_resource/{service_account_user}/flink_compute_pool"
    settings = get_secrets(aws_region, secret_name)

    # Build the ConfluentSettings object.
    confluent_settings = (
        ConfluentSettings
            .new_builder()
            .set_cloud(settings[FLINK_CLOUD])
            .set_region(settings[FLINK_REGION])
            .set_flink_api_key(settings[FLINK_API_KEY])
            .set_flink_api_secret(settings[FLINK_API_SECRET])
            .set_organization_id(settings[ORGANIZATION_ID])
            .set_environment_id(settings[ENVIRONMENT_ID])
            .set_compute_pool_id(settings[FLINK_COMPUTE_POOL_ID])
            .build()
    )
    tbl_env = TableEnvironment.create(confluent_settings)

    # The catalog name and database name are used to set the current catalog and database.
    catalog_name = f"{service_account_user}_env"
    database_name = f"{service_account_user}_kafka_cluster"
    tbl_env.use_catalog(catalog_name)
    tbl_env.use_database(database_name)
    catalog = tbl_env.get_catalog(catalog_name)

    # The Kafka sink table Confluent Cloud environment Table Descriptor with Avro serialization.
    flyer_stats_avro_table_descriptor = (
        ConfluentTableDescriptor
            .for_managed()
            .schema(
                Schema
                    .new_builder()
                    .column("email_address", DataTypes.STRING())
                    .column("total_flight_duration", DataTypes.INT())
                    .column("number_of_flights", DataTypes.INT())
                    .build())
            .distributed_by_into_buckets(1, "email_address")
            .key_format(FormatDescriptor.for_format("avro-registry").build())
            .value_format(FormatDescriptor.for_format("avro-registry").build())
            .build()
    )
    try:
        # Checks if the table exists.  If it does not, it will be created.
        flyer_stats_avro_table_path = ObjectPath(tbl_env.get_current_database(), "flyer_stats_avro")
        if not catalog.table_exists(flyer_stats_avro_table_path):
            tbl_env.create_table(
                flyer_stats_avro_table_path.get_full_name(),
                flyer_stats_avro_table_descriptor
            )
            print(f"Sink table '{flyer_stats_avro_table_path.get_full_name()}' created successfully.")
        else:
            print(f"Sink table '{flyer_stats_avro_table_path.get_full_name()}' already exists.")
    except Exception as e:
        print(f"A critical error occurred during the processing of the table because {e}")
        exit(1)