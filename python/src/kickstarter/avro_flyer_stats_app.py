from pyflink.datastream.window import TumblingEventTimeWindows, Time
from pyflink.datastream import StreamExecutionEnvironment, DataStream, TimeCharacteristic
from pyflink.table import StreamTableEnvironment
from pyflink.table.catalog import ObjectPath
import logging
import argparse

from model.flight_data import FlightData, FlyerStatsData
from helper.kafka_properties_udtf import execute_kafka_properties_udtf
from helper.flyer_stats_process_window_function import FlyerStatsProcessWindowFunction
from helper.common import load_catalog, load_database

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


# Setup the logger
logger = logging.getLogger('FlyerStatsApp')

def main(args):
    # Create a blank Flink execution environment
    env = StreamExecutionEnvironment.get_execution_environment()
    env.set_stream_time_characteristic(TimeCharacteristic.EventTime)
    
    ###
    # Enable checkpointing every 5000 milliseconds (5 seconds).  Note, consider the
    # resource cost of checkpointing frequency, as short intervals can lead to higher
    # I/O and CPU overhead.  Proper tuning of checkpoint intervals depends on the
    # state size, latency requirements, and resource constraints.
    ###
    env.enable_checkpointing(5000)

    ###
    # Set checkpoint timeout to 60 seconds, which is the maximum amount of time a
    # checkpoint attempt can take before being discarded.  Note, setting an appropriate
    # checkpoint timeout helps maintain a balance between achieving exactly-once semantics
    # and avoiding excessive delays that can impact real-time stream processing performance.
    ###
    env.get_checkpoint_config().set_checkpoint_timeout(60000)

    ###
    # Set the maximum number of concurrent checkpoints to 1 (i.e., only one checkpoint
    # is created at a time).  Note, this is useful for limiting resource usage and
    # ensuring checkpoints do not interfere with each other, but may impact throughput
    # if checkpointing is slow.  Adjust this setting based on the nature of your job,
    # the size of the state, and available resources. If your environment has enough
    # resources and you want to ensure faster recovery, you could increase the limit
    # to allow multiple concurrent checkpoints.
    ###
    env.get_checkpoint_config().set_max_concurrent_checkpoints(1)

    # Create a Table Environment
    tbl_env = StreamTableEnvironment.create(stream_execution_environment=env)

    # --- Load AWS Glue managed Apache Iceberg catalog.
    iceberg_catalog = load_catalog(tbl_env, args.aws_region, args.s3_bucket_name.replace("_", "-"), "apache_kickstarter")

    # --- Print the current catalog name.
    print(f"Current catalog: {tbl_env.get_current_catalog()}")

    # --- Load Iceberg database.
    load_database(tbl_env, iceberg_catalog, "airlines")

    # Print the current database name.
    print(f"Current database: {tbl_env.get_current_database()}")

    # Get the Kafka Cluster properties for the Kafka consumer and producer
    consumer_properties, registry_properties = execute_kafka_properties_udtf(tbl_env, True, args.s3_bucket_name)
    producer_properties, _ = execute_kafka_properties_udtf(tbl_env, False, args.s3_bucket_name)

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.flight_avro`
    table_name = "flight"
    topic_name = "airline.flight_avro"
    bootstrap_servers = consumer_properties.get("bootstrap.servers")
    sasl_jaas_config = consumer_properties.get("sasl.jaas.config").replace("'", "\\\"")
    schema_registry_url = registry_properties.get("schema.registry.url")
    basic_auth_credentials_source = registry_properties.get("schema.registry.basic.auth.credentials.source")
    basic_auth_user_info = registry_properties.get("schema.registry.basic.auth.user.info")

    # --- Check if the table exists.  If not, create it.
    kafka_source_table_path = ObjectPath(tbl_env.get_current_database(), f"{table_name}_kafka_source")
    try:
        tbl_env.execute_sql(f"""
            CREATE TABLE IF NOT EXISTS {kafka_source_table_path.get_full_name()} (
                email_address STRING,
                departure_time STRING,
                departure_airport_code STRING,
                arrival_time STRING,
                arrival_airport_code STRING,
                flight_number STRING,
                confirmation_code STRING,
                airline STRING
            ) WITH (
                'connector' = 'kafka',
                'topic' = '{topic_name}',
                'properties.bootstrap.servers' = '{bootstrap_servers}',
                'properties.sasl.jaas.config' = '{sasl_jaas_config}',
                'properties.group.id' = 'flight_avro_group',
                'properties.auto.offset.reset' = 'earliest',
                'scan.startup.mode' = 'earliest-offset',
                'properties.sasl.mechanism' = '{consumer_properties.get("sasl.mechanism")}',
                'properties.security.protocol' = '{consumer_properties.get("security.protocol")}',                
                'avro-confluent.basic-auth.credentials-source' = '{basic_auth_credentials_source}',
                'avro-confluent.basic-auth.user-info' = '{basic_auth_user_info}',
                'format' = 'avro-confluent',
                'value.format' = 'avro-confluent',
                'value.avro-confluent.url' = '{schema_registry_url}',
                'value.avro-confluent.subject' = '{topic_name}-value'
            )
        """)
    except Exception as e:
        print(f"A critical error occurred during the creation of the {kafka_source_table_path.get_full_name()} because {e}.")
        exit(1)

    # --- Query the table.
    source_table = tbl_env.sql_query(f"""
                                        SELECT 
                                            * 
                                        FROM 
                                            {kafka_source_table_path.get_full_name()}
                                     """)

    # --- Convert the Table to a DataStream of append-only data.
    flight_avro_stream = tbl_env.to_data_stream(source_table)

    # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.flyer_stats`
    topic_name = "airline.flyer_stats_avro"
    kafka_sink_table_path = ObjectPath(tbl_env.get_current_database(), "flyer_stats_kafka_sink")
    tbl_env.execute_sql(f"""
        CREATE TABLE IF NOT EXISTS {kafka_sink_table_path} (
            email_address STRING,
            total_flight_duration INT,
            number_of_flights INT
        ) WITH (
            'connector' = 'kafka',
            'topic' = '{topic_name}',
            'properties.bootstrap.servers' = '{bootstrap_servers}',
            'properties.sasl.jaas.config' = '{sasl_jaas_config}',
            'properties.sasl.mechanism' = '{producer_properties.get("sasl.mechanism")}',
            'properties.security.protocol' = '{producer_properties.get("security.protocol")}',
            'properties.client.dns.lookup' = '{producer_properties.get("client.dns.lookup")}',
            'properties.acks' = '{producer_properties.get("acks")}',
            'properties.transaction.timeout.ms' = '{producer_properties.get("transaction.timeout.ms")}',
            'sink.partitioner' = 'round-robin',
            'avro-confluent.basic-auth.credentials-source' = '{basic_auth_credentials_source}',
            'avro-confluent.basic-auth.user-info' = '{basic_auth_user_info}',
            'format' = 'avro-confluent',
            'value.format' = 'avro-confluent',
            'value.avro-confluent.url' = '{schema_registry_url}',
            'value.avro-confluent.subject' = '{topic_name}-value'                    
        )
    """)

    # An ObjectPath in Apache Flink is a class that represents the fully qualified path to a
    # catalog object, such as a table, view, or function.  It uniquely identifies an object
    # within a catalog by encapsulating both the database name and the object name.  For 
    # instance, this case we using it to get the fully qualified path of the `flyer_stats`
    # table
    stats_table_path = ObjectPath(tbl_env.get_current_database(), "flyer_stats")

    # Check if the table exists.  If not, create it
    try:
        if not iceberg_catalog.table_exists(stats_table_path):
            # Define the table using Flink SQL
            tbl_env.execute_sql(f"""
                CREATE TABLE {stats_table_path.get_full_name()} (
                    email_address STRING,
                    total_flight_duration INT,
                    number_of_flights INT
                ) WITH (
                    'write.format.default' = 'parquet',
                    'write.target-file-size-bytes' = '134217728',
                    'partitioning' = 'email_address',
                    'format-version' = '2'
                )
            """)
    except Exception as e:
        print(f"A critical error occurred to during the processing of the table because {e}")
        exit(1)

    # Define the workflow for the Flink job graph (DAG)
    flyer_stats_datastream = define_workflow(flight_avro_stream).map(lambda d: d.to_row(), output_type=FlyerStatsData.get_value_type_info())

    # Populate the table with the data from the data stream
    tbl_env.from_data_stream(flyer_stats_datastream).execute_insert(stats_table_path.get_full_name())

    # --- Populate the Apache Kafka Sink Topic with the data from the datastream.
    tbl_env.from_data_stream(flyer_stats_datastream).execute_insert(kafka_sink_table_path.get_full_name())

    # Execute the Flink job graph (DAG)
    try:
        env.execute("avro_flyer_stats_app")
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
            .map(FlightData.to_flyer_stats_data)    # Transforms each element in the datastream to a FlyerStatsData object
            .key_by(lambda s: s.email_address)          # Groups the data by email address
            .window(TumblingEventTimeWindows.of(Time.minutes(1)))   # Each window will contain all events that occur within that 1-minute period
            .reduce(FlyerStatsData.merge, window_function=FlyerStatsProcessWindowFunction())) # Applies a reduce function to each window


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--aws-s3-bucket',
                        dest='s3_bucket_name',
                        required=True,
                        help='The AWS S3 bucket name.')
    parser.add_argument('--aws-region',
                        dest='aws_region',
                        required=True,
                        help='The AWS Rgion name.')
    known_args, _ = parser.parse_known_args()
    main(known_args)