from pyflink.table import (TableEnvironment, Schema, DataTypes, FormatDescriptor)
from pyflink.table.catalog import ObjectPath
from pyflink.table.confluent import ConfluentSettings, ConfluentTableDescriptor
from pyflink.table.expressions import col, lit

from src.kickstarter.helper.settings import get_secrets, FLINK_CLOUD, FLINK_REGION, FLINK_COMPUTE_POOL_ID, FLINK_API_KEY, FLINK_API_SECRET, ORGANIZATION_ID, ENVIRONMENT_ID 


__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def run(args=None):
    service_account_user = "flink_kickstarter"
    secret_name = f"/confluent_cloud_resource/{service_account_user}/flink_compute_pool"
    settings = get_secrets("us-east-1", secret_name)

    confluent_settings = ConfluentSettings.new_builder() \
        .set_cloud(settings[FLINK_CLOUD]) \
        .set_region(settings[FLINK_REGION]) \
        .set_flink_api_key(settings[FLINK_API_KEY]) \
        .set_flink_api_secret(settings[FLINK_API_SECRET]) \
        .set_organization_id(settings[ORGANIZATION_ID]) \
        .set_environment_id(settings[ENVIRONMENT_ID]) \
        .set_compute_pool_id(settings[FLINK_COMPUTE_POOL_ID]) \
        .build()

    tbl_env = TableEnvironment.create(confluent_settings)

    catalog_name = "flink_kickstarter-env"
    database_name = "flink_kickstarter-kafka_cluster"

    tbl_env.use_catalog(catalog_name)
    tbl_env.use_database(database_name)
    catalog = tbl_env.get_catalog(catalog_name)

    # Checks if the table exists.  If it does not, it will be created.  The attributes of the table
    # are defined in the ConfluentTableDescriptor.for_managed() method.  The schema of the table is
    # defined in the Schema.new_builder() method.  The columns of the schema are defined in the
    # column() method.  The distributed_by_into_buckets() method is used to distribute the table
    # across 1 Kafka partitions based on the Kafka composite message key.  The key_format() and
    # value_format() methods are used to reference two Schema Registry subjects for Kafka message
    # key and value composed in the Avro data format.  The table is created with the create_table()
    # method.  If an exception occurs, the error is printed and the program exits.  The name of the
    # table is also the Kafka topic name.
    try:
        table_name = "flight_avro"
        flight_table_path = ObjectPath(tbl_env.get_current_database(), table_name)
        if not catalog.table_exists(flight_table_path):
            tbl_env.create_table(
                table_name,
                ConfluentTableDescriptor
                    .for_managed()
                    .schema(
                        Schema.new_builder()
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
    except Exception as e:
        print(f"A critical error occurred to during the processing of the table because {e}")
        exit(1)

    # The first table is the SkyOne table that is read in.
    skyone_airline = (
        tbl_env.from_path(f"`{catalog_name}`.`{database_name}`.`skyone_avro`")
            .select(col("email_address"), col("departure_time"), col("departure_airport_code"),
                    col("arrival_time"), col("arrival_airport_code"), col("flight_number"),
                    col("confirmation_code"), lit("SkyOne"))
    )

    # The second table is the Sunset table that is read in.
    sunset_airline = (
        tbl_env.from_path(f"`{catalog_name}`.`{database_name}`.`sunset_avro`")
            .select(col("email_address"), col("departure_time"), col("departure_airport_code"),
                    col("arrival_time"), col("arrival_airport_code"), col("flight_number"),
                    col("confirmation_code"), lit("Sunset"))
    )

    # The two tables are unioned together and the result is written to the flight_avro table.
    skyone_airline.union_all(sunset_airline).alias("email_address", "departure_time", "departure_airport_code",
                                                   "arrival_time", "arrival_airport_code", "flight_number",
                                                   "confirmation_code", "airline").execute()
