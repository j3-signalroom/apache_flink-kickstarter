from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.common.watermark_strategy import WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, TimeCharacteristic
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer
from pyflink.datastream.window import TumblingEventTimeWindows
from pyflink.java_gateway import get_gateway
import logging
from datetime import timedelta
import sys
import argparse

from model.flight_data import FlightData
from model.user_statistics_data import UserStatisticsData
from process_user_statistics_data_function import ProcessUserStatisticsDataFunction
from kafka_client_properties_lookup import KafkaClientPropertiesLookup

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


# Setup the logger
logger = logging.getLogger('UserStatisticsApp')

def main(args):
    # Create a blank Flink execution environment
    env = StreamExecutionEnvironment.get_execution_environment()
    env.set_stream_time_characteristic(TimeCharacteristic.EventTime)

    # Kafka Consumer Config
    options = {
        "consumer_kafka_client": True,
        "s3_bucket_name": args.s3_bucket_name
    }
    data_stream_consumer_properties = (
        env.from_collection([{}])
        .map(KafkaClientPropertiesLookup(flag=None, options=options))
        .name("kafka_consumer_properties")
    )

    consumer_properties = {}
    try:
        for type_value in data_stream_consumer_properties.execute_and_collect():
            consumer_properties.update(type_value)
    except Exception as e:
        print(f"The Flink App stopped during the reading of the custom data source stream because of the following: {e}")
        sys.exit(1)

    # Kafka Producer Config
    options = {
        "consumer_kafka_client": False,
        "s3_bucket_name": args.s3_bucket_name
    }
    data_stream_producer_properties = (
        env.from_collection([{}])
        .map(KafkaClientPropertiesLookup(flag=None, options=options))
        .name("kafka_producer_properties")
    )

    producer_properties = {}
    try:
        for type_value in data_stream_producer_properties.execute_and_collect():
            producer_properties.update(type_value)
    except Exception as e:
        print(f"The Flink App stopped during the reading of the custom data source stream because of the following: {e}")
        sys.exit(1)

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.all`
    flight_data_source = KafkaSource.builder() \
        .set_properties(consumer_properties) \
        .set_topics("airline.all") \
        .set_starting_offsets(KafkaOffsetsInitializer.earliest()) \
        .set_value_only_deserializer(JsonRowDeserializationSchema(FlightData)) \
        .build()

    # Takes the results of the Kafka source and attaches the unbounded data stream
    flight_data_stream = env.from_source(flight_data_source, WatermarkStrategy.for_monotonous_timestamps(), "flightdata_source")

    # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.user_statistics`
    statistics_serializer = KafkaRecordSerializationSchema.builder() \
        .set_topic("airline.user_statistics") \
        .set_value_serialization_schema(JsonRowSerializationSchema(UserStatisticsData)) \
        .build()

    stats_sink = KafkaSink.builder() \
        .set_kafka_producer_config(producer_properties) \
        .set_record_serializer(statistics_serializer) \
        .set_delivery_guarantee(KafkaSink.DeliveryGuarantee.AT_LEAST_ONCE) \
        .build()

    # Defines the workflow for the Flink job graph (DAG) by connecting the data streams
    define_workflow(flight_data_stream).sink_to(stats_sink).name("userstatistics_sink").uid("userstatistics_sink")

    try:
        env.execute("UserStatisticsApp")
    except Exception as e:
        logger.error("The App stopped early due to the following: %s", e)

def define_workflow(flight_data_source):
    return flight_data_source \
        .map(lambda flight: UserStatisticsData(flight)) \
        .key_by(lambda stats: stats.email_address) \
        .window(TumblingEventTimeWindows.of(timedelta(minutes=1))) \
        .reduce(lambda a, b: a.merge(b), ProcessUserStatisticsDataFunction())


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--aws_s3_bucket',
        dest='s3_bucket_name',
        required=True,
        help='The AWS S3 bucket name.')
    known_args, _ = parser.parse_known_args()
    main(known_args)
