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
    flight_avro_table_descriptor = (
        ConfluentTableDescriptor
            .for_managed()
            .schema(
                Schema
                    .new_builder()
                    .column("departure_airport_code", DataTypes.STRING())
                    .column("flight_number", DataTypes.STRING())
                    .column("email_address", DataTypes.STRING())
                    .column("departure_time", DataTypes.STRING())
                    .column("arrival_time", DataTypes.STRING())
                    .column("arrival_airport_code", DataTypes.STRING())
                    .column("confirmation_code", DataTypes.STRING())
                    .column("airline", DataTypes.STRING())
                    .build())
            .distributed_by_into_buckets(1, "departure_airport_code", "flight_number")
            .key_format(FormatDescriptor.for_format("avro-registry").build())
            .value_format(FormatDescriptor.for_format("avro-registry").build())
            .build()
    )
    try:
        # Checks if the table exists.  If it does not, it will be created.
        flight_table_path = ObjectPath(tbl_env.get_current_database(), "flight_avro")
        if not catalog.table_exists(flight_table_path):
            tbl_env.create_table(
                flight_table_path.get_full_name(),
                flight_avro_table_descriptor
            )
            print(f"Sink table '{flight_table_path}' created successfully.")
        else:
            print(f"Sink table '{flight_table_path}' already exists.")
    except Exception as e:
        print(f"A critical error occurred during the processing of the table because {e}")
        exit(1)

    # The first table is the SkyOne table that is read in.
    skyone_airline = (
        tbl_env.from_path(f"{catalog_name}.{database_name}.skyone_avro")
            .select(
                col("email_address"), 
                col("departure_time"), 
                col("departure_airport_code"),
                col("arrival_time"), 
                col("arrival_airport_code"), 
                col("flight_number"),
                col("confirmation_code"),
                lit("SkyOne")
            )
    )

    # The second table is the Sunset table that is read in.
    sunset_airline = (
        tbl_env.from_path(f"{catalog_name}.{database_name}.sunset_avro")
            .select(
                col("email_address"), 
                col("departure_time"), 
                col("departure_airport_code"),
                col("arrival_time"), 
                col("arrival_airport_code"), 
                col("flight_number"),
                col("confirmation_code"),
                lit("Sunset")
            )
    )

    # Combine the two tables.
    combined_airlines = (
        skyone_airline.union_all(sunset_airline)
        .alias(
            "departure_airport_code", 
            "flight_number",
            "email_address", 
            "departure_time",
            "arrival_time",
            "arrival_airport_code",
            "confirmation_code", 
            "airline"
        )
        .filter(
            col("email_address").is_not_null & 
            col("departure_time").is_not_null & 
            col("departure_airport_code").is_not_null &
            col("arrival_time").is_not_null &  
            col("arrival_airport_code").is_not_null &  
            col("flight_number").is_not_null & 
            col("confirmation_code").is_not_null
        )
    )

    # Insert the combined record into the sink table.
    try:
        combined_airlines.execute_insert(flight_table_path.get_full_name()).wait()
    except Exception as e:
        print(f"An error occurred during data insertion: {e}")
        exit(1)
