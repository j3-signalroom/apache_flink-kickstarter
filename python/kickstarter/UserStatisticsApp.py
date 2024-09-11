# Copyright (c) 2024 Jeffrey Jonathan Jennings
# Author: Jeffrey Jonathan Jennings (J3)
#
# This class processes data from the `airline.all` Kafka topic to aggregate user
# statistics in the `airline.user_statistics` Kafka topic.

from pyflink.common.serialization import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.common.watermark_strategy import WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, TimeCharacteristic
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema
from pyflink.datastream.window import TumblingEventTimeWindows
from pyflink.datastream.functions import RuntimeContext, ProcessWindowFunction
from apache_flink.kickstarter import KafkaClientPropertiesLookup, Common
from apache_flink.kickstarter.model import FlightData, UserStatisticsData
from apache_flink.kickstarter.functions import ProcessUserStatisticsDataFunction
import json
import logging
from datetime import timedelta

# Setup the logger
logger = logging.getLogger('UserStatisticsApp')

class UserStatisticsApp:

    @staticmethod
    def main(args):
        # Create a blank Flink execution environment
        env = StreamExecutionEnvironment.get_execution_environment()
        env.set_stream_time_characteristic(TimeCharacteristic.EventTime)

        # Kafka Consumer Config
        data_stream_consumer_properties = (
            env.from_collection([{}])
            .map(KafkaClientPropertiesLookup(True, Common.get_app_options(args)))
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
            .map(KafkaClientPropertiesLookup(False, Common.get_app_options(args)))
            .name("kafka_producer_properties")
        )

        producer_properties = {}
        try:
            for type_value in data_stream_producer_properties.execute_and_collect():
                producer_properties.update(type_value)
        except Exception as e:
            print(f"The Flink App stopped during the reading of the custom data source stream because of the following: {e}")
            exit(1)

        # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.all`
        flight_data_source = KafkaSource.builder() \
            .set_properties(consumer_properties) \
            .set_topics("airline.all") \
            .set_starting_offsets(OffsetsInitializer.earliest()) \
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
            .set_delivery_guarantee(DeliveryGuarantee.AT_LEAST_ONCE) \
            .build()

        # Defines the workflow for the Flink job graph (DAG) by connecting the data streams
        UserStatisticsApp.define_workflow(flight_data_stream).sink_to(stats_sink).name("userstatistics_sink").uid("userstatistics_sink")

        try:
            env.execute("UserStatisticsApp")
        except Exception as e:
            logger.error("The App stopped early due to the following: %s", e)

    @staticmethod
    def define_workflow(flight_data_source):
        return flight_data_source \
            .map(lambda flight: UserStatisticsData(flight)) \
            .key_by(lambda stats: stats.email_address) \
            .window(TumblingEventTimeWindows.of(timedelta(minutes=1))) \
            .reduce(lambda a, b: a.merge(b), ProcessUserStatisticsDataFunction())


if __name__ == "__main__":
    import sys
    UserStatisticsApp.main(sys.argv)
