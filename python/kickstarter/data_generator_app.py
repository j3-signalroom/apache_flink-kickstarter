from pyflink.common import Types, Row
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table.table_environment import StreamTableEnvironment
from pyflink.table import TableSchema, DataTypes, TableEnvironment, EnvironmentSettings, ExplainDetail
from pyflink.table.catalog import ObjectPath, CatalogBaseTable
import logging
import os
import sys
import time

from common_functions import get_app_options
from data_generator import DataGenerator


__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


# Set up the logger
logger = logging.getLogger('DataGeneratorApp')


class DataGeneratorApp:
    """
    This class generates synthetic flight data for two fictional airlines, Sunset Air
    and SkyOne Airlines. The synthetic flight data is stored in separate Apache Iceberg
    tables for Sunset Air and SkyOne Airlines.
    """
    
    @staticmethod
    def main(args):
        # Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG)
        env = StreamExecutionEnvironment.get_execution_environment()

        env.set_parallelism(1)

        # Add the custom source
        data_stream_producer_properties = env.add_source(KafkaClientPropertiesLookup(flag=None, options=get_app_options(False, args)))

        producer_properties = {}
        try:
            for type_value in data_stream_producer_properties.execute_and_collect():
                producer_properties.update(type_value)
        except Exception as e:
            print(f"The Flink App stopped during the reading of the custom data source stream because of the following: {e}")
            exit(1)
        
        # Set up the table environment to work with the Table and SQL API in Flink
        table_env = StreamTableEnvironment.create(env, EnvironmentSettings.in_streaming_mode())

        # Define the placheolder values for the preceeding Flink SQL statements
        catalog_name = "apache_kickstarter"
        database_name = "airlines"
        s3_bucket = os.environ['AWS_S3_BUCKET']
        aws_region = os.environ['AWS_REGION']
        skyone_airlines_table = "skyone_airlines"
        sunset_air_table = "sunset_airlines"

        # Define the CREATE CATALOG Flink SQL statement to register the Iceberg catalog
        # using the HadoopCatalog to store metadata in AWS S3, a Hadoop-compatible 
        # filesystem.  Then execute the Flink SQL statement to register the Iceberg catalog
        table_env.execute_sql(f"""
            CREATE CATALOG {catalog_name} WITH (
                'type'='iceberg',
                'catalog-type'='hadoop',
                'catalog-impl'='org.apache.iceberg.flink.FlinkCatalog',
                'warehouse'='{s3_bucket}',
                'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO',
                'aws.region' = '{aws_region}',
                's3.endpoint' = 's3.{aws_region}.amazonaws.com'
                );
        """)
        table_env.execute_sql(f"USE CATALOG {catalog_name};")

        # Access the Iceberg catalog to create the airlines database and the Iceberg tables
        catalog = table_env.get_catalog(catalog_name).get()

        # Check if the database exists.  If it does not exist, create the database
        try:
            if not catalog.database_exists(database_name):
                table_env.execute_sql(f"CREATE DATABASE IF NOT EXISTS {database_name};")
                table_env.execute_sql(f"USE {database_name};")
        except Exception as e:
            print(f"A critical error occurred to during the processing of the database because {e}")
            sys.exit(1)
        
        # An ObjectPath in Apache Flink is a class that represents the fully qualified path to a
        # catalog object, such as a table, view, or function.  It uniquely identifies an object
        # within a catalog by encapsulating both the database name and the object name.  For 
        # instance, this case we using it to get the fully qualified path of the `skyone_airlines`
        # table
        skyone_airlines_table_path = ObjectPath(database_name, skyone_airlines_table)

        # Check if the table exists.  If it does not exist, create the table
        try:
            if not catalog.table_exists(skyone_airlines_table_path):
                # Define the table schema
                schema = TableSchema.builder() \
                    .field("email_address", DataTypes.STRING()) \
                    .field("flight_departure_time", DataTypes.TIMESTAMP(3)) \
                    .field("iata_departure_code", DataTypes.STRING()) \
                    .field("flight_arrival_time", DataTypes.TIMESTAMP(3)) \
                    .field("iata_arrival_code", DataTypes.STRING()) \
                    .field("flight_number", DataTypes.STRING()) \
                    .field("confirmation", DataTypes.STRING()) \
                    .field("ticket_price", DataTypes.STRING()) \
                    .field("aircraft", DataTypes.STRING()) \
                    .field("booking_agency_email", DataTypes.STRING()) \
                    .build()
                
                # Define Apache Iceberg-specific table properties
                properties = {
                    'connector': 'iceberg',
                    'catalog-type': 'hadoop',  # Type of Iceberg catalog (used for working with AWS S3)
                    'warehouse': f"'{s3_bucket}'",  # Warehouse directory where Iceberg stores data and metadata
                    'write.format.default': 'parquet',  # File format for Iceberg writes
                    'write.target-file-size-bytes': '134217728',  # Target size for files written by Iceberg (128 MB by default)
                    'partitioning': 'iata_arrival_code'  # Optional: Partitioning columns for Iceberg table
                }

                # Create a CatalogBaseTable instance, which is an instantiated object that represents the
                # metadata of a table within a catalog.  It encapsulates all the necessary information
                # about a table's schema, properties, and characteristics, allowing Flink to interact
                # with various data sources and sinks in a unified and consistent manner
                catalog_table = CatalogBaseTable.create_table(schema=schema, properties=properties, comment="The SkyOne Airlines table")

                # Create the table in the catalog
                catalog.create_table(skyone_airlines_table_path, catalog_table, ignore_if_exists=True)
        except Exception as e:
            print(f"A critical error occurred to during the processing of the table because {e}")
            sys.exit(1)

        # An ObjectPath in Apache Flink is a class that represents the fully qualified path to a
        # catalog object, such as a table, view, or function.  It uniquely identifies an object
        # within a catalog by encapsulating both the database name and the object name.  For 
        # instance, this case we using it to get the fully qualified path of the `sunset_air`
        # table
        sunset_air_table_path = ObjectPath(database_name, sunset_air_table)

        # Check if the table exists.  If it does not exist, create the table
        try:
            if not catalog.table_exists(sunset_air_table_path):
                # Create a CatalogBaseTable instance, which is an instantiated object that represents the
                # metadata of a table within a catalog.  It encapsulates all the necessary information
                # about a table's schema, properties, and characteristics, allowing Flink to interact
                # with various data sources and sinks in a unified and consistent manner
                catalog_table = CatalogBaseTable.create_table(schema=schema, properties=properties, comment="The Sunset Air table")

                # Create the table in the catalog
                catalog.create_table(sunset_air_table_path, catalog_table, ignore_if_exists=True)
        except Exception as e:
            print(f"A critical error occurred to during the processing of the table because {e}")
            sys.exit(1)

        # Insert the row of synthetic flight data into the SkyOne Airlines table
        data_gen = DataGenerator()
        row = data_gen.generate_skyone_airlines_flight_data()
        table_env.from_elements([row.to_row()], schema=schema) \
            .execute_insert(skyone_airlines_table_path)
        
        # Creates a rate limiter that allows emitting one event per second.  This is useful for
        # controlling the rate of data generation in real-world scenarios, preventing overloading
        # downstream operators or external systems, and controlling the flow of data to test system
        # behavior under specific conditions
        time.sleep(1)

        # Insert the row of synthetic flight data into the Sunset Air table
        data_gen = DataGenerator()
        row = data_gen.generate_sunset_air_flight_data()
        table_env.from_elements([row.to_row()], schema=schema) \
            .execute_insert(sunset_air_table_path)
        
        # Creates a rate limiter that allows emitting one event per second.  This is useful for
        # controlling the rate of data generation in real-world scenarios, preventing overloading
        # downstream operators or external systems, and controlling the flow of data to test system
        # behavior under specific conditions
        time.sleep(1)

        # Execute the Flink job graph
        try:
            env.execute("DataGeneratorApp")
        except Exception as e:
            logger.error("The App stopped early due to the following: %s", e)


if __name__ == "__main__":
    DataGeneratorApp.main(sys.argv)
