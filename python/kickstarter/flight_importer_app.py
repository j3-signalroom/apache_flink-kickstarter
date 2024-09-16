from pyflink.common import WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, MapFunction
from pyflink.table import EnvironmentSettings, StreamTableEnvironment
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer
from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.datastream.functions import RuntimeContext
from pyflink.java_gateway import get_gateway
from datetime import datetime
import logging
import argparse

from common_functions import get_mapper
from model.skyone_airlines_flight_data import SkyOneAirlinesFlightData
from model.sunset_air_flight_data import SunsetAirFlightData
from kafka_client_properties_lookup import KafkaClientPropertiesLookup

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

    # Kafka Consumer Config
    options = {
        "is_consumer": True,
        "s3_bucket_name": args.s3_bucket_name
    }
    data_stream_consumer_properties = env.add_source(KafkaClientPropertiesLookup(s3_bucket_name=args.s3_bucket_name, options=options))

    consumer_properties = {}
    try:
        for type_value in data_stream_consumer_properties.execute_and_collect():
            consumer_properties.update(type_value)
    except Exception as e:
        print(f"The Flink App stopped during the reading of the custom data source stream because of the following: {e}")
        exit(1)

    # Kafka Producer Config
    options = {
        "consumer_kafka_client": False,
        "s3_bucket_name": args.s3_bucket_name
    }
    data_stream_producer_properties = env.add_source(KafkaClientPropertiesLookup(s3_bucket_name=args.s3_bucket_name, options=options))

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
        '--service-account-user',
        dest='s3_bucket_name',
        required=True,
        help='The service account user name is used as the name for the AWS S3 bucket.')
    known_args, _ = parser.parse_known_args()
    main(known_args)
