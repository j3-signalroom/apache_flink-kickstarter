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

        while True:
            data_gen = DataGenerator()
            items = data_gen.generate_items()
            len_sky = len([item for item in items if item.__class__.__name__ == "SkyOneAirlinesFlightData"])
            logging.info(f"{len_sky} items from SkyOne and {len(items) - len_sky} from Sunset")
            print(f"{len_sky} items from SkyOne and {len(items) - len_sky} from Sunset")
            """
            for item in items:
                try:
                    if item.__class__.__name__ == "SkyOneAirlinesFlightData":
                        topic_name = "skyone"
                        key = {"ref": item.confirmation}
                        arrival_time = item.flight_arrival_time
                    else:
                        topic_name = "sunset"
                        key = {"ref": item.reference_number}
                        arrival_time = item.arrival_time
                    self.producer_client.send(
                        topic=topic_name,
                        key=key,
                        value=item.asdict(),
                    )
                    logging.info(
                        f"record sent, topic - {topic_name}, ref - {key['ref']}, arrival time - {arrival_time} "
                    )
                except Exception as err:
                    raise RuntimeError("fails to send a message") from err
            logging.info(f"wait for {wait_for} seconds...")
            time.sleep(wait_for)
            """

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
            .set_value_serialization_schema(JsonRowSerializationSchema(common_functions.get_mapper())) \
            .build()

        skyone_sink = KafkaSink.builder() \
            .set_kafka_producer_config(producer_properties) \
            .set_record_serializer(skyone_serializer) \
            .set_delivery_guarantee(KafkaSink.DeliveryGuarantee.AT_LEAST_ONCE) \
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
            .set_value_serialization_schema(JsonRowSerializationSchema(common_functions.get_mapper())) \
            .build()

        sunset_sink = KafkaSink.builder() \
            .set_kafka_producer_config(producer_properties) \
            .set_record_serializer(sunset_serializer) \
            .set_delivery_guarantee(KafkaSink.DeliveryGuarantee.AT_LEAST_ONCE) \
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
