from pyflink.common import WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, DataStream
from pyflink.table import EnvironmentSettings, StreamTableEnvironment
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, JsonDeserializationSchema, JsonSerializationSchema, KafkaOffsetsInitializer
from pyflink.datastream.functions import RuntimeContext
from datetime import datetime
from kafka_client_properties_lookup import KafkaClientPropertiesLookup
import common_functions as common_functions
from model import FlightData, SkyOneAirlinesFlightData, SunsetAirFlightData
import logging
import os

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


# Setup the logger
logger = logging.getLogger('FlightImporterApp')

class FlightImporterApp:

    @staticmethod
    def main(args):
        # Create a blank Flink execution environment
        env = StreamExecutionEnvironment.get_execution_environment()

        # Kafka Consumer Config
        data_stream_consumer_properties = (
            env.from_collection([{}])
            .map(KafkaClientPropertiesLookup(False, common_functions.get_app_options(args)))
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

        # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.skyone`
        skyone_source = KafkaSource.builder() \
            .set_properties(consumer_properties) \
            .set_topics("airline.skyone") \
            .set_starting_offsets(KafkaOffsetsInitializer.earliest()) \
            .set_value_only_deserializer(JsonDeserializationSchema(SkyOneAirlinesFlightData)) \
            .build()

        # Takes the results of the Kafka source and attaches the unbounded data stream
        skyone_stream = env.from_source(skyone_source, WatermarkStrategy.for_monotonous_timestamps(), "skyone_source")

        # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.sunset`
        sunset_source = KafkaSource.builder() \
            .set_properties(consumer_properties) \
            .set_topics("airline.sunset") \
            .set_starting_offsets(KafkaOffsetsInitializer.earliest()) \
            .set_value_only_deserializer(JsonDeserializationSchema(SunsetAirFlightData)) \
            .build()

        # Takes the results of the Kafka source and attaches the unbounded data stream
        sunset_stream = env.from_source(sunset_source, WatermarkStrategy.for_monotonous_timestamps(), "sunset_source")

        # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.all`
        flight_serializer = KafkaRecordSerializationSchema.builder() \
            .set_topic("airline.all") \
            .set_value_serialization_schema(JsonSerializationSchema(common_functions.get_mapper())) \
            .build()

        flight_sink = KafkaSink.builder() \
            .set_kafka_producer_config(producer_properties) \
            .set_record_serializer(flight_serializer) \
            .build()

        # Defines the workflow for the Flink job graph (DAG) by connecting the data streams
        FlightImporterApp.define_workflow(skyone_stream, sunset_stream).sink_to(flight_sink).name("flightdata_sink")

        try:
            env.execute("FlightImporterApp")
        except Exception as e:
            logger.error("The App stopped early due to the following: %s", e)

    @staticmethod
    def define_workflow(skyone_source, sunset_source):
        skyone_flight_stream = skyone_source \
            .filter(lambda flight: flight.get_flight_arrival_time() > datetime.now()) \
            .map(SkyOneAirlinesFlightData.to_flight_data)

        sunset_flight_stream = sunset_source \
            .filter(lambda flight: flight.get_arrival_time() > datetime.now()) \
            .map(SunsetAirFlightData.to_flight_data)

        return skyone_flight_stream.union(sunset_flight_stream)

if __name__ == "__main__":
    import sys
    FlightImporterApp.main(sys.argv)
