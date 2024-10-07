from pyflink.common import WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, DataStream
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer, DeliveryGuarantee
from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.table import StreamTableEnvironment
from pyflink.table.catalog import ObjectPath
from datetime import datetime, timezone
import argparse

from model.flight_data import FlightData
from model.skyone_airline_flight_data import SkyOneAirlinesFlightData
from model.sunset_airline_flight_data import SunsetAirFlightData
from helper.kafka_properties import execute_kafka_properties_udtf

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def main(args):
    """The entry point to the Flight Importer Flink App (a.k.a., Flink job graph --- DAG).
        
    Args:
        args (str): is the arguments passed to the script.
    """
    # Create a blank Flink execution environment
    env = StreamExecutionEnvironment.get_execution_environment()

    # Create a Table Environment
    tbl_env = StreamTableEnvironment.create(stream_execution_environment=env)

    # Adjust resource configuration
    env.set_parallelism(1)  # Set parallelism to 1 for simplicity

    # Get the Kafka Cluster properties for the Kafka consumer client
    consumer_properties = execute_kafka_properties_udtf(tbl_env, True, args.s3_bucket_name)

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.skyone`
    # Note: KafkaSource was introduced in Flink 1.14.0.  If you are using an older version of Flink, 
    # you will need to use the FlinkKafkaConsumer class.
    skyone_source = (KafkaSource.builder()
                                .set_properties(consumer_properties)
                                .set_topics("airline.skyone")
                                .set_group_id("skyone_group")
                                .set_starting_offsets(KafkaOffsetsInitializer.earliest())
                                .set_value_only_deserializer(JsonRowDeserializationSchema
                                                             .builder()
                                                             .type_info(SkyOneAirlinesFlightData.get_value_type_info())
                                                             .build())
                                .build())

    # Takes the results of the Kafka source and attaches the unbounded data stream
    skyone_stream = (env.from_source(skyone_source, WatermarkStrategy.no_watermarks(), "skyone_source")
                        .uid("skyone_source"))

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.sunset`
    sunset_source = (KafkaSource.builder()
                                .set_properties(consumer_properties)
                                .set_topics("airline.sunset")
                                .set_group_id("sunset_group")
                                .set_starting_offsets(KafkaOffsetsInitializer.earliest())
                                .set_value_only_deserializer(JsonRowDeserializationSchema
                                                             .builder()
                                                             .type_info(SunsetAirFlightData.get_value_type_info())
                                                             .build())
                                .build())

    # Takes the results of the Kafka source and attaches the unbounded data stream
    sunset_stream = (env.from_source(sunset_source, WatermarkStrategy.no_watermarks(), "sunset_source")
                        .uid("sunset_source"))

    # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.all`
    # Get the Kafka Cluster properties for the producer
    producer_properties = execute_kafka_properties_udtf(tbl_env, False, args.s3_bucket_name)
    producer_properties.update({
        'transaction.timeout.ms': '60000'  # Set transaction timeout to 60 seconds
    })

    # Note: KafkaSink was introduced in Flink 1.14.0.  If you are using an older version of Flink, 
    # you will need to use the FlinkKafkaProducer class.
    flight_sink = (KafkaSink.builder()
                            .set_bootstrap_servers(producer_properties['bootstrap.servers'])
                            .set_property("security.protocol", producer_properties['security.protocol'])
                            .set_property("sasl.mechanism", producer_properties['sasl.mechanism'])
                            .set_property("sasl.jaas.config", producer_properties['sasl.jaas.config'])
                            .set_property("acks", producer_properties['acks'])
                            .set_property("client.dns.lookup", producer_properties['client.dns.lookup'])
                            .set_property("transaction.timeout.ms", producer_properties['transaction.timeout.ms'])
                            .set_record_serializer(KafkaRecordSerializationSchema
                                                   .builder()
                                                   .set_topic("airline.all")
                                                   .set_value_serialization_schema(JsonRowSerializationSchema
                                                                                   .builder()
                                                                                   .with_type_info(FlightData.get_value_type_info())
                                                                                   .build())
                                                   .build())
                            .set_delivery_guarantee(DeliveryGuarantee.EXACTLY_ONCE)
                            .build())
    
    # Define the CREATE CATALOG Flink SQL statement to register the Iceberg catalog
    # using the HadoopCatalog to store metadata in AWS S3 (i.e., s3a://), a Hadoop- 
    # compatible filesystem.  Then execute the Flink SQL statement to register the
    # Iceberg catalog
    catalog_name = "apache_kickstarter"
    bucket_name = args.s3_bucket_name.replace("_", "-") # To follow S3 bucket naming convention, replace underscores with hyphens if exist
    try:
        catalog_result = tbl_env.execute_sql(f"""
            CREATE CATALOG {catalog_name} WITH (
                'type' = 'iceberg',
                'catalog-type' = 'hadoop',            
                'warehouse' = 's3a://{bucket_name}/warehouse',
                'property-version' = '1',
                'io-impl' = 'org.apache.iceberg.hadoop.HadoopFileIO'
                );
        """)
    except Exception as e:
        print(f"A critical error occurred to during the processing of the catalog because {e}")
        exit(1)

    # Use the Iceberg catalog
    tbl_env.use_catalog(catalog_name)

    # Access the Iceberg catalog to create the airlines database and the Iceberg tables
    catalog = tbl_env.get_catalog(catalog_name)

    # Print the current catalog name
    print(f"Current catalog: {tbl_env.get_current_catalog()}")

    # Check if the database exists.  If not, create it
    database_name = "airlines"
    try:
        if not catalog.database_exists(database_name):
            tbl_env.execute_sql(f"CREATE DATABASE IF NOT EXISTS {database_name};")
        else:
            print(f"The {database_name} database already exists.")
        tbl_env.execute_sql(f"USE {database_name};")
    except Exception as e:
        print(f"A critical error occurred to during the processing of the database because {e}")
        exit(1)

    # Print the current database name
    print(f"Current database: {tbl_env.get_current_database()}")

    # An ObjectPath in Apache Flink is a class that represents the fully qualified path to a
    # catalog object, such as a table, view, or function.  It uniquely identifies an object
    # within a catalog by encapsulating both the database name and the object name.  For 
    # instance, this case we using it to get the fully qualified path of the `flight`
    # table
    flight_table_path = ObjectPath(database_name, "flight")

    # Check if the table exists.  If not, create it
    try:
        if not catalog.table_exists(flight_table_path):
            # Define the table using Flink SQL
            tbl_env.execute_sql(f"""
                CREATE TABLE {flight_table_path.get_full_name()} (
                    email_address STRING,
                    departure_time STRING,
                    departure_airport_code STRING,
                    arrival_time STRING,
                    arrival_airport_code STRING,
                    flight_number STRING,
                    confirmation STRING,
                    source STRING
                ) WITH (
                    'write.format.default' = 'parquet',
                    'write.target-file-size-bytes' = '134217728',
                    'partitioning' = 'arrival_airport_code',
                    'format-version' = '2'
                )
            """)
    except Exception as e:
        print(f"A critical error occurred to during the processing of the table because {e}")
        exit(1)

    # Define the workflow for the Flink job graph (DAG)
    flight_datastream = define_workflow(skyone_stream, sunset_stream).map(lambda d: d.to_row(), output_type=FlightData.get_value_type_info())

    # Populate the table with the data from the data stream
    (tbl_env.from_data_stream(flight_datastream)
            .execute_insert(flight_table_path.get_full_name()))

    # Sinks the Flight DataStream into a single Kafka topic
    (flight_datastream.sink_to(flight_sink)
                      .name("flightdata_sink")
                      .uid("flightdata_sink"))
    
    # Execute the Flink job graph (DAG)
    try:
        env.execute("FlightImporterApp")
    except Exception as e:
        print(f"The App stopped early due to the following: {e}.")


def define_workflow(skyone_stream: DataStream, sunset_stream: DataStream) -> DataStream:
    """This method defines the workflow for the Flink job graph (DAG) by connecting the data streams.

    Args:
        skyone_stream (DataStream): is the source of the SkyOne Airlines flight data.
        sunset_stream (DataStream): is the source of the Sunset Air flight data.

    Returns:
        DataStream: the union of the SkyOne Airlines and Sunset Air flight data streams.
    """
    # Map the data streams to the FlightData model and filter out Skyone flights that have already arrived
    skyone_flight_stream = (skyone_stream
                            .map(SkyOneAirlinesFlightData.to_flight_data)
                            .filter(lambda flight: datetime.fromisoformat(flight.arrival_time) > datetime.now(timezone.utc)))

    # Map the data streams to the FlightData model and filter out Sunset flights that have already arrived
    sunset_flight_stream = (sunset_stream
                            .map(SunsetAirFlightData.to_flight_data)
                            .filter(lambda flight: datetime.fromisoformat(flight.arrival_time) > datetime.now(timezone.utc)))
    
    # Return the union of the two data streams
    return skyone_flight_stream.union(sunset_flight_stream)


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
