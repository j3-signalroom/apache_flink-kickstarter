from pyflink.datastream import StreamExecutionEnvironment
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema
from pyflink.common import Types
from pyflink.datastream.formats.json import JsonRowSerializationSchema
from pyflink.common.watermark_strategy import WatermarkStrategy
from pyflink.table import (DataTypes, TableEnvironment, EnvironmentSettings, ExplainDetail)
import common_functions as common_functions
from kafka_client_properties_lookup import KafkaClientPropertiesLookup
from data_generator import DataGenerator
import logging


__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@signalroom.ai"
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

        t_env = TableEnvironment.create(EnvironmentSettings.in_streaming_mode())

        data_gen = DataGenerator()

        # Create a data generator source for SkyOne Airlines
        items = data_gen.generate_skyone_airlines_flight_data()
        len_sky = len([item for item in items if item.__class__.__name__ == "SkyOneAirlinesFlightData"])
        logging.info(f"{len_sky} items from SkyOne and {len(items) - len_sky} from Sunset")
        print(f"{len_sky} items from SkyOne and {len(items) - len_sky} from Sunset")

if __name__ == "__main__":
    import sys
    DataGeneratorApp.main(sys.argv)
