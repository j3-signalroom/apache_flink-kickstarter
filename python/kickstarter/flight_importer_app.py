from pyflink.common import WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, DataStream
from pyflink.java_gateway import get_gateway
from pyflink.table import EnvironmentSettings, StreamTableEnvironment
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer
from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.datastream.functions import RuntimeContext
from pyflink.common.typeinfo import Types
from datetime import datetime
import logging
import argparse

from common_functions import get_mapper
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

    # Path to your custom JAR file
    custom_jar_path = "/opt/flink/python_apps/java_classes/kickstarter.jar"

    # Add the JAR to the classpath
    env.add_jars(f"file://{custom_jar_path}")

    # Get the Java Gateway and JVM
    gateway = get_gateway()
    jvm = gateway.jvm

    # Create an empty DataStream
    data_stream = env.from_collection([])

    # Access the underlying Java DataStream
    java_data_stream = data_stream._j_data_stream

    # Fully qualified class name of your Java RichMapFunction
    class_name = 'kickstarter/KafkaClientPropertiesLookup'

    # Load and instantiate the Java RichMapFunction for Kafka Consumer Config
    KafkaClientPropertiesLookupClass = jvm.Thread.currentThread().getContextClassLoader().loadClass(class_name)
    constructor = KafkaClientPropertiesLookupClass.getConstructor(jvm.Boolean, jvm.String)
    kafka_client_properties_lookup_class_instance = constructor.newInstance(True, args.s3_bucket)

    # Apply the Java RichMapFunction to the Java DataStream
    mapped_java_data_stream = java_data_stream.map(kafka_client_properties_lookup_class_instance)

    # Wrap the Java DataStream back into a PyFlink DataStream
    data_stream_consumer_properties = DataStream(mapped_java_data_stream, env)

    # Provide type information
    data_stream_consumer_properties = data_stream_consumer_properties.map(
        lambda x: x,  # Identity function
        output_type= Types.TUPLE()
    )

    consumer_properties = {}
    try:
        for type_value in data_stream_consumer_properties.execute_and_collect():
            consumer_properties.update(type_value)
    except Exception as e:
        print(f"The Flink App stopped during the reading of the custom data source stream because of the following: {e}")
        exit(1)

    # Load and instantiate the Java RichMapFunction for Kafka Producer Config
    KafkaClientPropertiesLookupClass = jvm.Thread.currentThread().getContextClassLoader().loadClass(class_name)
    kafka_client_properties_lookup_class_instance = constructor.newInstance(False, args.s3_bucket)

    # Apply the Java RichMapFunction to the Java DataStream
    mapped_java_data_stream = java_data_stream.map(kafka_client_properties_lookup_class_instance)

    # Wrap the Java DataStream back into a PyFlink DataStream
    data_stream_producer_properties = DataStream(mapped_java_data_stream, env)

    # Provide type information
    data_stream_producer_properties = data_stream_producer_properties.map(
        lambda x: x,  # Identity function
        output_type= Types.TUPLE()
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
