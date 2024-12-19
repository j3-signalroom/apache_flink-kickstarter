from pyflink.table import (TableEnvironment, Schema, DataTypes, FormatDescriptor)
from pyflink.table.confluent import ConfluentSettings, ConfluentTableDescriptor
from pyflink.table.expressions import col

from src.kickstarter.helper.settings import CC_PROPERTIES_PATHNAME

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def run(args=None):
    settings = ConfluentSettings.from_file(CC_PROPERTIES_PATHNAME)

    tbl_env = TableEnvironment.create(settings)

    catalog_name = "flink_kickstarter-env"
    database_name = "flink_kickstarter-kafka_cluster"

    tbl_env.use_catalog(catalog_name)
    tbl_env.use_database(database_name)

    # Create a table programmatically with following attributes:
    #   - is backed by an equally named Kafka topic
    #   - stores its payload in AVRO
    #   - will reference two Schema Registry subjects for Kafka message key and value
    #   - is distributed across 1 Kafka partitions based on the Kafka composite message key ""
    tbl_env.create_table(
        "flight_avro_ccaf_4",
        ConfluentTableDescriptor.for_managed()
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

    # For this example, we read both target tables in again and union them into one output to
    # verify that the data arrives
    skyone_airline = (
        tbl_env.from_path("skyone_avro")
        .select(col("email_address"), col("departure_time"), col("departure_airport_code"),
                col("arrival_time"), col("arrival_airport_code"), col("flight_number"),
                col("confirmation_code"), "SkyOne")
    )

    sunset_airline = (
        tbl_env.from_path("sunset_avro")
        .select(col("email_address"), col("departure_time"), col("departure_airport_code"),
                col("arrival_time"), col("arrival_airport_code"), col("flight_number"),
                col("confirmation_code"), "Sunset ")
    )

    skyone_airline.union(sunset_airline).alias("email_address", "departure_time", "departure_airport_code",
                                               "arrival_time", "arrival_airport_code", "flight_number",
                                               "confirmation_code", "airline").execute()
