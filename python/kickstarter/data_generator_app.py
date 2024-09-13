from pyflink.common import Types
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table import DataTypes, TableEnvironment, EnvironmentSettings, ExplainDetail
from pyflink.table.catalog import ObjectPath
import logging
import os
import common_functions as common_functions
from kafka_client_properties_lookup import KafkaClientPropertiesLookup
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

        # Kafka Producer Config
        data_stream_producer_properties = (
            env.from_collection([{}])
            .map(KafkaClientPropertiesLookup(False, common_functions.get_app_options(args)))
            .name("kafka_producer_properties")
        )

        producer_properties = {}
        try:
            for type_value in data_stream_producer_properties.execute_and_collect():
                producer_properties.update(type_value)
        except Exception as e:
            print(f"The Flink App stopped during the reading of the custom data source stream because of the following: {e}")
            exit(1)

        # Get a row of SkyOne Airlines synthetic flight data
        data_gen = DataGenerator()
        row = data_gen.generate_skyone_airlines_flight_data()
        
        # Set up the table environment to work with the Table and SQL API in Flink
        table_env = StreamTableEnvironment.create(env, EnvironmentSettings.in_streaming_mode())

        # Define the CREATE CATALOG Flink SQL statement to register the Iceberg catalog
        # using the HadoopCatalog to store metadata in AWS S3, a Hadoop-compatible 
        # filesystem
        flink_sql = "CREATE CATALOG apache_kickstarter WITH (\n" + \
                    "  'type'='iceberg',\n" + \
                    "  'catalog-type'='hadoop',\n" + \
                    "  'catalog-impl'='org.apache.iceberg.flink.FlinkCatalog',\n" + \
                    "  'warehouse'='{s3_bucket}',\n" + \
                    "  'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO',\n" + \
                    "  'aws.region' = '{aws_region}',\n" + \
                    "  's3.endpoint' = 's3.{aws_region}.amazonaws.com');".format(s3_bucket=os.environ['AWS_S3_BUCKET'],
                                                                                 aws_region=os.environ['AWS_REGION'])
        
        # Execution the registration of the Iceberg catalog with Flink SQL in the DAG
        table_env.execute_sql(flink_sql)



if __name__ == "__main__":
    import sys
    DataGeneratorApp.main(sys.argv)
