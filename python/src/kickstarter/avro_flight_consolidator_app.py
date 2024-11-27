from pyflink.datastream import StreamExecutionEnvironment, DataStream
from pyflink.table import StreamTableEnvironment
from pyflink.table.catalog import ObjectPath
from datetime import datetime, timezone
import argparse

from model.flight_data import FlightData
from model.airline_flight_data import AirlineFlightData
from helper.confluent_properties_udtf import execute_confluent_properties_udtf
from helper.common import parse_isoformat, load_catalog, load_database

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
    # --- Create a blank Flink execution environment
    env = StreamExecutionEnvironment.get_execution_environment()

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

    # Retrieve the Kafka Cluster properties for the Kafka consumer and producer, and
    # the Schema Registry Cluster properties.
    consumer_properties, registry_properties = execute_confluent_properties_udtf(tbl_env, True, args.s3_bucket_name)
    producer_properties, _ = execute_confluent_properties_udtf(tbl_env, False, args.s3_bucket_name)

    # Takes the results of the Kafka source and attaches the unbounded data stream
    skyone_stream = read_kafka_topic_records(env, consumer_properties, registry_properties, "airline.skyone", "airlines", "skyone")
    sunset_stream = read_kafka_topic_records(env, consumer_properties, registry_properties, "airline.sunset", "airlines", "sunset")    

    # --- Load Apache Iceberg catalog
    catalog = load_catalog(tbl_env, args.aws_region, args.s3_bucket_name.replace("_", "-"), "apache_kickstarter")

    # --- Print the current catalog name
    print(f"Current catalog: {tbl_env.get_current_catalog()}")

    # --- Load database
    load_database(tbl_env, catalog, "airlines")

    # Print the current database name
    print(f"Current database: {tbl_env.get_current_database()}")

    # An ObjectPath in Apache Flink is a class that represents the fully qualified path to a
    # catalog object, such as a table, view, or function.  It uniquely identifies an object
    # within a catalog by encapsulating both the database name and the object name.  For 
    # instance, this case we using it to get the fully qualified path of the `flight`
    # table
    flight_table_path = ObjectPath(tbl_env.get_current_database(), "flight")

    # Print the current table name
    print(f"Current table: {flight_table_path.get_full_name()}")

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
                    airline STRING
                ) WITH (
                    'write.format.default' = 'parquet',
                    'write.target-file-size-bytes' = '134217728',
                    'partitioning' = 'arrival_airport_code',
                    'format-version' = '2'
                )
            """)

            # Create the table that is link to Kafka Topic sink
            tbl_env.execute_sql(f"""
                CREATE TABLE airline.flight (
                    email_address STRING,
                    departure_time STRING,
                    departure_airport_code STRING,
                    arrival_time STRING,
                    arrival_airport_code STRING,
                    flight_number STRING,
                    confirmation STRING,
                    airline STRING
                ) WITH (
                    'connector' = 'kafka',
                    'topic' = 'airline.flight',
                    'properties.bootstrap.servers' = '{producer_properties.get('bootstrap.servers')}',
                    'properties.sasl.mechanism' = '{producer_properties.get('sasl.mechanism')}',
                    'properties.security.protocol' = '{producer_properties.get('security.protocol')}',
                    'properties.sasl.jaas.config' = '{producer_properties.get('sasl.jaas.config')}',
                    'properties.client.dns.lookup' = '{producer_properties.get('client.dns.lookup')}',
                    'properties.acks' = '{producer_properties.get('acks')}',
                    'properties.transaction.timeout.ms' = '{producer_properties.get('transaction.timeout.ms')}',
                    'format' = 'avro-confluent',
                    'value.format' = 'avro-confluent',
                    'value.avro-confluent.url' = '{registry_properties.get('schema.registry.url')}',
                    'avro-confluent.schema-registry.url' = '{registry_properties.get('schema.registry.url')}',
                    'avro-confluent.schema-registry.basic-auth.credentials-source' = '{registry_properties.get('schema.registry.basic.auth.credentials.source')}',
                    'avro-confluent.schema-registry.basic-auth.user-info' = '{registry_properties.get('schema.registry.basic.auth.user.info')}',
                    'sink.partitioner' = 'round-robin'                    
                )
            """)
    except Exception as e:
        print(f"A critical error occurred to during the processing of the table because {e}")
        exit(1)
       
    # Combine the Airline DataStreams into one DataStream
    flight_datastream = combine_datastreams(skyone_stream, sunset_stream).map(lambda d: d.to_row(), output_type=FlightData.get_value_type_info())

    # Populate the Apache Iceberg Table with the data from the data stream
    (tbl_env.from_data_stream(flight_datastream)
            .execute_insert(flight_table_path.get_full_name()))

    # Populate the Apache Iceberg Table with the data from the data stream
    (tbl_env.from_data_stream(flight_datastream)
            .execute_insert("airline.flight"))
    
    # Execute the Flink job graph (DAG)
    try:
        env.execute("avro_flight_consolidator_app")
    except Exception as e:
        print(f"The App stopped early due to the following: {e}.")


def combine_datastreams(skyone_stream: DataStream, sunset_stream: DataStream) -> DataStream:
    """This function combines the SkyOne Airlines and Sunset Air flight data streams into
    a single data stream.

    Args:
        skyone_stream (DataStream): is the source of the SkyOne Airlines flight data.
        sunset_stream (DataStream): is the source of the Sunset Air flight data.

    Returns:
        DataStream: the union of the SkyOne Airlines and Sunset Air flight data streams.
    """
    # Map the data streams to the FlightData model and filter out Skyone flights that have already arrived
    skyone_flight_stream = (skyone_stream
                            .map(lambda flight: AirlineFlightData.to_flight_data("SkyOne", flight))
                            .filter(lambda flight: parse_isoformat(flight.arrival_time) > datetime.now(timezone.utc)))

    # Map the data streams to the FlightData model and filter out Sunset flights that have already arrived
    sunset_flight_stream = (sunset_stream
                            .map(lambda flight: AirlineFlightData.to_flight_data("Sunset", flight))
                            .filter(lambda flight: parse_isoformat(flight.arrival_time) > datetime.now(timezone.utc)))
    
    # Return the union of the two data streams
    return skyone_flight_stream.union(sunset_flight_stream)


def read_kafka_topic_records(tbl_env: StreamTableEnvironment, consumer_properties: dict, registry_properties: dict, group_id: str, topic_name: str, database_name: str, table_name: str) -> DataStream:

    # Check if the table exists.  If not, create it
    try:
        tbl_env.execute_sql(f"""
            CREATE TABLE {database_name}.{table_name} (
                email_address STRING,
                departure_time STRING,
                departure_airport_code STRING,
                arrival_time STRING,
                arrival_airport_code STRING,
                flight_number STRING,
                confirmation STRING,
                ticket_price DECIMAL,
                aircraft STRING,
                booking_agency_email STRING
            ) WITH (
                'connector' = 'kafka',
                'topic' = '{topic_name}',
                'properties.bootstrap.servers' = '{consumer_properties.get('bootstrap.servers')}',
                'properties.sasl.jaas.config' = '{consumer_properties.get('sasl.jaas.config')}',
                'properties.group.id' = '{group_id}',
                'scan.startup.mode' = 'earliest-offset',
                'format' = 'avro-confluent',
                'value.format' = 'avro-confluent',
                'value.avro-confluent.url' = '{registry_properties.get('schema.registry.url')}',
                'avro-confluent.schema-registry.url' = '{registry_properties.get('schema.registry.url')}',
                'avro-confluent.schema-registry.basic-auth.credentials-source' = '{registry_properties.get('schema.registry.basic.auth.credentials.source')}',
                'avro-confluent.schema-registry.basic-auth.user-info' = '{registry_properties.get('schema.registry.basic.auth.user.info')}'
            )
        """)
    except Exception as e:
        print(f"A critical error occurred during the creation of the {database_name}.{table_name} because {e}.")
        exit(1)

    return tbl_env.to_data_stream(f"{database_name}.{table_name}")


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
