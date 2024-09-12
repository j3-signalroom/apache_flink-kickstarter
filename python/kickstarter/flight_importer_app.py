from pyflink.common import WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, DataStream
from pyflink.table import EnvironmentSettings, StreamTableEnvironment
from pyflink.datastream.connectors import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, JsonDeserializationSchema, JsonSerializationSchema
from pyflink.datastream.functions import RuntimeContext
from kafka_client_properties_lookup import KafkaClientPropertiesLookup
import python.kickstarter.common_functions as common_functions
from model import FlightData, SkyOneAirlinesFlightData, SunsetAirFlightData
import logging
import os

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@signalroom.ai"
__status__     = "dev"


# Setup the logger
logger = logging.getLogger('FlightImporterApp')

class FlightImporterApp:
    """
    This class imports flight data from `airline.sunset` and `airline.skyone` Kafka topics
    and converts it to a unified format for the `airline.all` Kafka topic.
    """

    @staticmethod
    def main(args):
        # Create a blank Flink execution environment
        env = StreamExecutionEnvironment.get_execution_environment()

        # Set up the table environment to work with the Table and SQL API in Flink
        table_env = StreamTableEnvironment.create(env, EnvironmentSettings.in_streaming_mode())

        # Create an Apache Iceberg Catalog using Project Nessie
        table_env.execute_sql(
            """
            CREATE CATALOG iceberg WITH (
                'type'='iceberg',
                'catalog-impl'='org.apache.iceberg.nessie.NessieCatalog',
                'uri'='http://localhost:19120/api/v1',
                'ref'='main',
                'warehouse'='/opt/flink/data/warehouse'
            )
            """
        )

        # List all available Apache Iceberg Catalogs
        result = table_env.execute_sql("SHOW CATALOGS")
        result.print()

        # Kafka Consumer Config
        data_stream_consumer_properties = (
            env.from_collection([{}])
            .map(KafkaClientPropertiesLookup(True, common_functions.get_app_options(args)))
            .name("kafka_consumer_properties")
        )

        consumer_properties = {}
        try:
            for type_value in data_stream_consumer_properties.execute_and_collect():
                consumer_properties.update(type_value)
        except Exception as e:
            print(f"The Flink App stopped during the reading of the custom data source stream because of the following: {e}")
            exit(1)

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

        # Sets up a Flink Kafka source to consume data from the
