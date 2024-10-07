from pyflink.common import WatermarkStrategy
from pyflink.datastream.window import TumblingEventTimeWindows, Time
from pyflink.datastream import StreamExecutionEnvironment, DataStream, TimeCharacteristic
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer, DeliveryGuarantee
from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.table import StreamTableEnvironment
import logging
import argparse

from model.flight_data import FlightData, UserStatisticsData
from helper.kafka_properties import execute_kafka_properties_udtf
from helper.process_user_statistics_data_function import ProcessUserStatisticsDataFunction

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

    # Create a Table Environment
    tbl_env = StreamTableEnvironment.create(stream_execution_environment=env)

    # Get the Kafka Cluster properties for the consumer
    consumer_properties = execute_kafka_properties_udtf(tbl_env, True, args.s3_bucket_name)

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.all`
    flight_source = (KafkaSource.builder()
                                .set_properties(consumer_properties)
                                .set_topics("airline.all")
                                .set_group_id("flight_group")
                                .set_starting_offsets(KafkaOffsetsInitializer.earliest())
                                .set_value_only_deserializer(JsonRowDeserializationSchema
                                                             .builder()
                                                             .type_info(FlightData.get_value_type_info())
                                                             .build())
                                .build())

    # Takes the results of the Kafka source and attaches the unbounded data stream
    flight_data_stream = env.from_source(flight_source, WatermarkStrategy.for_monotonous_timestamps(), "flight_data_source")

    # Get the Kafka Cluster properties for the producer
    producer_properties = execute_kafka_properties_udtf(tbl_env, False, args.s3_bucket_name)
    producer_properties.update({
        'transaction.timeout.ms': '60000'  # Set transaction timeout to 60 seconds
    })

    # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.user_statistics`
    stats_sink = (KafkaSink.builder()
                           .set_bootstrap_servers(producer_properties['bootstrap.servers'])
                           .set_property("security.protocol", producer_properties['security.protocol'])
                           .set_property("sasl.mechanism", producer_properties['sasl.mechanism'])
                           .set_property("sasl.jaas.config", producer_properties['sasl.jaas.config'])
                           .set_property("acks", producer_properties['acks'])
                           .set_property("client.dns.lookup", producer_properties['client.dns.lookup'])
                           .set_property("transaction.timeout.ms", producer_properties['transaction.timeout.ms'])
                           .set_record_serializer(KafkaRecordSerializationSchema
                                                  .builder()
                                                  .set_topic("airline.user_statistics")
                                                  .set_value_serialization_schema(JsonRowSerializationSchema
                                                                                  .builder()
                                                                                  .with_type_info(UserStatisticsData.get_value_type_info())
                                                                                  .build())
                                                  .build())
                            .set_delivery_guarantee(DeliveryGuarantee.EXACTLY_ONCE)
                            .build())

    # Defines the workflow for the Flink job graph (DAG) by connecting the data streams
    (define_workflow(flight_data_stream).map(lambda d: d.to_row(), output_type=UserStatisticsData.get_value_type_info())
                                        .sink_to(stats_sink)
                                        .name("userstatistics_sink")
                                        .uid("userstatistics_sink"))

    try:
        env.execute("UserStatisticsApp")
    except Exception as e:
        logger.error("The App stopped early due to the following: %s", e)

def define_workflow(flight_data_stream: DataStream) -> DataStream:
    """This method defines a data processing workflow for a stream of flight data using Apache
    Flink.  This workflow processes the data to compute user statistics over tumbling time
    windows.

    Args:
        flight_data_stream (DataStream): The datastream that will have a workflow defined for it.

    Returns:
        DataStream: The defined workflow of the inputted datastream.
    """
    return (flight_data_stream
            .map(FlightData.to_user_statistics_data)    # Transforms each element in the datastream to a UserStatisticsData object
            .key_by(lambda s: s.email_address)          # Groups the data by email address
            .window(TumblingEventTimeWindows.of(Time.minutes(1)))   # Each window will contain all events that occur within that 1-minute period
            .reduce(UserStatisticsData.merge, window_function=ProcessUserStatisticsDataFunction())) # Applies a reduce function to each window


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--aws_s3_bucket',
                        dest='s3_bucket_name',
                        required=True,
                        help='The AWS S3 bucket name.')
    parser.add_argument('--aws_region',
                        dest='aws_region',
                        required=True,
                        help='The AWS Region name.')
    known_args, _ = parser.parse_known_args()
    main(known_args)
