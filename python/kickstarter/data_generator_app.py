from pyflink.datastream import StreamExecutionEnvironment, RateLimiterStrategy
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema
from pyflink.common import Types, Long
from pyflink.common.serialization import JsonRowSerializationSchema
from pyflink.datastream.connectors.datagen import DataGeneratorSource
from pyflink.common.watermark_strategy import WatermarkStrategy
import python.kickstarter.common_functions as common_functions
from kafka_client_properties_lookup import KafkaClientPropertiesLookup
from data_generator import DataGenerator
from model import SkyOneAirlinesFlightData, SunsetAirFlightData
from enums import DeliveryGuarantee
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
    This class creates fake flight data for fictional airlines Sunset Air and Sky One Airlines
    and sends it to the Kafka topics `airline.sunset` and `airline.skyone`, respectively.
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

        # Create a data generator source for SkyOne Airlines
        skyone_source = DataGeneratorSource(
            lambda index: DataGenerator.generate_skyone_airlines_flight_data(),
            Long.MAX_VALUE,
            RateLimiterStrategy.per_second(1),
            Types.PICKLE(SkyOneAirlinesFlightData)
        )

        # Sets up a Flink POJO source to consume data
        skyone_stream = env.from_source(skyone_source, WatermarkStrategy.no_watermarks(), "skyone_source")

        # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.skyone`
        skyone_serializer = KafkaRecordSerializationSchema.builder() \
            .set_topic("airline.skyone") \
            .set_value_serialization_schema(JsonRowSerializationSchema(common_functions.get_mapper)) \
            .build()

        skyone_sink = KafkaSink.builder() \
            .set_kafka_producer_config(producer_properties) \
            .set_record_serializer(skyone_serializer) \
            .set_delivery_guarantee(DeliveryGuarantee.AT_LEAST_ONCE) \
            .build()

        skyone_stream.sink_to(skyone_sink).name("skyone_sink")

        # Create a data generator source for Sunset Air
        sunset_source = DataGeneratorSource(
            lambda index: DataGenerator.generate_sunset_air_flight_data(),
            Long.MAX_VALUE,
            RateLimiterStrategy.per_second(1),
            Types.PICKLE(SunsetAirFlightData)
        )

        sunset_stream = env.from_source(sunset_source, WatermarkStrategy.no_watermarks(), "sunset_source")

        # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.sunset`
        sunset_serializer = KafkaRecordSerializationSchema.builder() \
            .set_topic("airline.sunset") \
            .set_value_serialization_schema(JsonRowSerializationSchema(common_functions.get_mapper)) \
            .build()

        sunset_sink = KafkaSink.builder() \
            .set_kafka_producer_config(producer_properties) \
            .set_record_serializer(sunset_serializer) \
            .set_delivery_guarantee(DeliveryGuarantee.AT_LEAST_ONCE) \
            .build()

        sunset_stream.sink_to(sunset_sink).name("sunset_sink")

        try:
            # Execute the Flink job graph (DAG)
            env.execute("DataGeneratorApp")
        except Exception as e:
            print(f"The Flink App stopped early due to the following: {e}")

if __name__ == "__main__":
    import sys
    DataGeneratorApp.main(sys.argv)
