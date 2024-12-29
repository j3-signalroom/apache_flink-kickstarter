from pyflink.table import TableEnvironment, Schema, DataTypes, FormatDescriptor
from pyflink.table.catalog import ObjectPath
from pyflink.table.confluent import ConfluentSettings, ConfluentTableDescriptor, ConfluentTools
from pyflink.table.expressions import col, lit
import argparse
import uuid
from functools import reduce

from ccaf.helper.settings import get_secrets, FLINK_CLOUD, FLINK_REGION, FLINK_COMPUTE_POOL_ID, FLINK_API_KEY, FLINK_API_SECRET, ORGANIZATION_ID, ENVIRONMENT_ID 


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

    Returns:
        None
    """
    # The service account user is passed in as a command line argument.  
    parser = argparse.ArgumentParser()
    parser.add_argument('--catalog-name',
                        dest='catalog_name',
                        required=True,
                        help='The Catalog Name.')
    parser.add_argument('--database-name',
                        dest='database_name',
                        required=True,
                        help='The Database Name.')
    parser.add_argument('--aws-region',
                        dest='aws_region',
                        required=True,
                        help='The AWS Region.')
    known_args, _ = parser.parse_known_args()
    catalog_name = known_args.catalog_name.lower()
    database_name = known_args.database_name.lower()
    aws_region = known_args.aws_region.lower()

    # Retrieve the Confluent Cloud settings from AWS Secrets Manager.
    secret_name = f"/confluent_cloud_resource/{catalog_name}/flink_compute_pool"
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
        flight_avro_table_path = ObjectPath(tbl_env.get_current_database(), "flight_avro")
        if not catalog.table_exists(flight_avro_table_path):
            tbl_env.create_table(
                flight_avro_table_path.get_full_name(),
                flight_avro_table_descriptor
            )
            print(f"Sink table '{flight_avro_table_path.get_full_name()}' created successfully.")
        else:
            print(f"Sink table '{flight_avro_table_path.get_full_name()}' already exists.")
    except Exception as e:
        print(f"A critical error occurred during the processing of the table because {e}")
        exit(1)

    # The first table is the SkyOne table that is read in.
    airline = tbl_env.from_path(f"{catalog_name}.{database_name}.skyone_avro")

    # Get the schema and columns from the airline table.
    schema = airline.get_schema()

    # The columns that are not needed in the table the represents general airline flight data.
    exclude_airline_columns = ["key", "flight_duration", "ticket_price", "aircraft", "booking_agency_email", "$rowtime"]
    
    # Get only the columns that are not in the excluded columns list.
    flight_expressions = [col(field) for field in schema.get_field_names() if field not in exclude_airline_columns]
    flight_columns = [field for field in schema.get_field_names() if field not in exclude_airline_columns]

    # The first table is the SkyOne table that is read in.
    skyone_airline = airline.select(*flight_expressions, lit("SkyOne"))

    # The second table is the Sunset table that is read in.
    sunset_airline = airline.select(*flight_expressions, lit("Sunset"))

    # Build a compound expression, ensuring each column is not null
    filter_condition = reduce(
        lambda accumulated_columns, current_column: accumulated_columns & col(current_column).is_not_null, flight_columns[1:], col(flight_columns[0]).is_not_null
    )

    # Combine the two tables.
    combined_airlines = (
        skyone_airline.union_all(sunset_airline)
        .alias(*flight_columns, "airline")
        .filter(filter_condition)
    )

    # Insert the combined record into the sink table.
    try:
        statement_name = "combined-flight-data-" + str(uuid.uuid4())
        tbl_env.get_config().set("client.statement-name", statement_name)
        combined_airlines.execute_insert(flight_avro_table_path.get_full_name()).wait()
    except Exception as e:
        print(f"An error occurred during data insertion: {e}")
        exit(1)
