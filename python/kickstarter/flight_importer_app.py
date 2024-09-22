from typing import Iterator
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table import StreamTableEnvironment
from pyflink.common import WatermarkStrategy
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer
from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from datetime import datetime
import logging
import argparse

from common_functions import get_mapper
from helper.kafka_properties import get_kafka_properties
from model.skyone_airlines_flight_data import SkyOneAirlinesFlightData
from model.sunset_air_flight_data import SunsetAirFlightData

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


# Setup the logger
logger = logging.getLogger('FlightImporterApp')

def main(args):
    # Create a blank Flink execution environment
    env = StreamExecutionEnvironment.get_execution_environment()
    tbl_env = StreamTableEnvironment.create(env)

    # Adjust resource configuration
    env.set_parallelism(1)  # Set parallelism to 1 for simplicity

    consumer_properties = get_kafka_properties(tbl_env, True, args.s3_bucket_name)

    producer_properties = get_kafka_properties(tbl_env, False, args.s3_bucket_name)

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.skyone`
    skyone_source = KafkaSource.builder() \
        .set_properties(consumer_properties) \
        .set_topics("airline.skyone") \
        .set_starting_offsets(KafkaOffsetsInitializer.earliest()) \
        .set_value_only_deserializer(JsonRowDeserializationSchema(SkyOneAirlinesFlightData)) \
        .build()

    # Takes the results of the Kafka source and attaches the unbounded data stream
    skyone_stream = env.from_source(skyone_source, WatermarkStrategy.for_monotonous_timestamps(), "skyone_source")

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.sunset`
    sunset_source = KafkaSource.builder() \
        .set_properties(consumer_properties) \
        .set_topics("airline.sunset") \
        .set_starting_offsets(KafkaOffsetsInitializer.earliest()) \
        .set_value_only_deserializer(JsonRowDeserializationSchema(SunsetAirFlightData)) \
        .build()

    # Takes the results of the Kafka source and attaches the unbounded data stream
    sunset_stream = env.from_source(sunset_source, WatermarkStrategy.for_monotonous_timestamps(), "sunset_source")

    # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.all`
    flight_serializer = KafkaRecordSerializationSchema.builder() \
        .set_topic("airline.all") \
        .set_value_serialization_schema(JsonRowSerializationSchema( get_mapper())) \
        .build()

    flight_sink = KafkaSink.builder() \
        .set_kafka_producer_config(producer_properties) \
        .set_record_serializer(flight_serializer) \
        .build()

    # Defines the workflow for the Flink job graph (DAG) by connecting the data streams
    define_workflow(skyone_stream, sunset_stream).sink_to(flight_sink).name("flightdata_sink")

    try:
        env.execute("FlightImporterApp")
    except Exception as e:
        logger.error("The App stopped early due to the following: %s", e)

def define_workflow(skyone_source, sunset_source):
    skyone_flight_stream = skyone_source \
        .filter(lambda flight: flight.get_flight_arrival_time() > datetime.now()) \
        .map(SkyOneAirlinesFlightData.to_flight_data)

    sunset_flight_stream = sunset_source \
        .filter(lambda flight: flight.get_arrival_time() > datetime.now()) \
        .map(SunsetAirFlightData.to_flight_data)

    return skyone_flight_stream.union(sunset_flight_stream)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--aws_s3_bucket',
        dest='s3_bucket_name',
        required=True,
        help='The AWS S3 bucket name.')
    known_args, _ = parser.parse_known_args()
    main(known_args)
