import os
from pyflink.common import Configuration, WatermarkStrategy
from pyflink.datastream import StreamExecutionEnvironment, DataStream
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaOffsetsInitializer
from pyflink.datastream.formats.json import JsonRowDeserializationSchema
from pyflink.datastream.checkpoint_config import ExternalizedCheckpointRetention
from pyflink.table import StreamTableEnvironment
from pyflink.table.catalog import ObjectPath
from datetime import datetime, timezone
import logging

from model.flight_data import FlightData
from model.airline_flight_data import AirlineFlightData
from helper.common import load_catalog, load_database, get_confluent_properties


__copyright__  = "Copyright (c) 2024-2025 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


# Setup the logger
logger = logging.getLogger('FlyerStatsApp')


def main():
    """The entry point to the Flight Importer Flink App (a.k.a., Flink job graph --- DAG)."""
    # --- Retrieve environment variables
    service_account_user = os.getenv("SERVICE_ACCOUNT_USER", "")
    aws_s3_bucket = os.getenv("AWS_S3_BUCKET", "")
    aws_region = os.getenv("AWS_REGION", "")

    # Get the Kafka Client properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
    secret_path_prefix = f"confluent_cloud_resource/{service_account_user}"

    """
    Using the `java_client` configuration instead of the `python_client` configuration
    because PyFlink converts into Java code and the Java code is what is executed.
    """
    consumer_properties, error_message = get_confluent_properties(
        aws_region_name=aws_region,
        kafka_cluster_secrets_path=f"{secret_path_prefix}/kafka_cluster/java_client",
        client_parameters_path=f"/{secret_path_prefix}/consumer_kafka_client"
    )
    if error_message:
        raise RuntimeError(f"Failed to retrieve the Confluent Cloud properties for the Kafka Consumer client because {error_message}")
    producer_properties, error_message = get_confluent_properties(
        aws_region_name=aws_region,
        kafka_cluster_secrets_path=f"{secret_path_prefix}/kafka_cluster/java_client",
        client_parameters_path=f"/{secret_path_prefix}/producer_kafka_client"
    )
    if error_message:
        raise RuntimeError(f"Failed to retrieve the Confluent Cloud properties for the Kafka Producer client because {error_message}")
    producer_properties['compression.type'] = 'lz4'

    # --- Create a configuration to force Avro serialization instead of Kyro serialization.
    config = Configuration()
    config.set_string("pipeline.force-avro", "true")

    # --- Configure parent-first classloading for metrics
    config.set_string("classloader.parent-first-patterns.additional", 
                    "com.codahale.metrics;io.dropwizard.metrics")

    # --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG).
    env = StreamExecutionEnvironment.get_execution_environment(config)

    """
    Enable checkpointing every 10,000 milliseconds (10 seconds).  Note, consider the
    resource cost of checkpointing frequency, as short intervals can lead to higher
    I/O and CPU overhead.  Proper tuning of checkpoint intervals depends on the
    state size, latency requirements, and resource constraints.
    """
    env.enable_checkpointing(10000)

    # --- Set minimum pause between checkpoints to 5,000 milliseconds (5 seconds).
    env.get_checkpoint_config().set_min_pause_between_checkpoints(5000)

    # --- Set tolerable checkpoint failure number to 3.
    env.get_checkpoint_config().set_tolerable_checkpoint_failure_number(3)

    """
    Externalized Checkpoint Retention: RETAIN_ON_CANCELLATION" means that the system will keep the
    checkpoint data in persistent storage even if the job is manually canceled. This allows you to
    later restore the job from that last saved state, which is different from the default behavior,
    where checkpoints are deleted on cancellation. This setting requires you to manually clean up
    the checkpoint state later if it's no longer needed. 
    """
    env.get_checkpoint_config().set_externalized_checkpoint_retention(
        ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION
    )

    """
    Set checkpoint timeout to 60 seconds, which is the maximum amount of time a
    checkpoint attempt can take before being discarded.  Note, setting an appropriate
    checkpoint timeout helps maintain a balance between achieving exactly-once semantics
    and avoiding excessive delays that can impact real-time stream processing performance.
    """
    env.get_checkpoint_config().set_checkpoint_timeout(60000)

    """
    Set the maximum number of concurrent checkpoints to 1 (i.e., only one checkpoint
    is created at a time).  Note, this is useful for limiting resource usage and
    ensuring checkpoints do not interfere with each other, but may impact throughput
    if checkpointing is slow.  Adjust this setting based on the nature of your job,
    the size of the state, and available resources. If your environment has enough
    resources and you want to ensure faster recovery, you could increase the limit
    to allow multiple concurrent checkpoints.
    """
    env.get_checkpoint_config().set_max_concurrent_checkpoints(1)

    # Create a Table Environment
    tbl_env = StreamTableEnvironment.create(stream_execution_environment=env)

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.skyone`
    skyone_source = (KafkaSource.builder()
                                .set_properties(consumer_properties)
                                .set_topics("airline.skyone")
                                .set_group_id("skyone_group")
                                .set_starting_offsets(KafkaOffsetsInitializer.earliest())
                                .set_value_only_deserializer(JsonRowDeserializationSchema
                                                             .builder()
                                                             .type_info(AirlineFlightData.get_value_type_info())
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
                                                             .type_info(AirlineFlightData.get_value_type_info())
                                                             .build())
                                .build())

    # Takes the results of the Kafka source and attaches the unbounded data stream
    sunset_stream = (env.from_source(sunset_source, WatermarkStrategy.no_watermarks(), "sunset_source")
                        .uid("sunset_source"))

    # --- Load Apache Iceberg catalog
    catalog = load_catalog(tbl_env, aws_region, aws_s3_bucket, "apache_kickstarter")

    logging.info("Current catalog: %s", tbl_env.get_current_catalog())

    # --- Load database
    load_database(tbl_env, catalog, "airlines")

    logging.info("Current database: %s", tbl_env.get_current_database())

    """
    An ObjectPath in Apache Flink is a class that represents the fully qualified path to a
    catalog object, such as a table, view, or function.  It uniquely identifies an object
    within a catalog by encapsulating both the database name and the object name.  For
    instance, this case we using it to get the fully qualified path of the `flight`
    table
    """
    flight_table_path = ObjectPath(tbl_env.get_current_database(), "flight")

    logging.info("Current table: %s", flight_table_path.get_full_name())

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
                ) PARTITIONED BY (arrival_airport_code)
                WITH (
                    'write.format.default' = 'parquet',
                    'write.target-file-size-bytes' = '134217728',
                    'write.delete.mode' = 'merge-on-read',
                    'write.update.mode' = 'merge-on-read',
                    'format-version' = '2'
                )
            """)

    except Exception as e:
        logging.error("A critical error occurred to during the processing of the table because %s", e)
        exit(1)

    # Combine the Airline DataStreams into one DataStream
    flight_datastream = combine_datastreams(skyone_stream, sunset_stream).map(
            lambda d: d.to_row(), output_type=FlightData.get_value_type_info()
        ).name("combined_flight_datastream").uid("combined_flight_datastream")
    
    # Create a temporary view from the datastream
    tbl_env.create_temporary_view("temp_flight_view", flight_datastream)

    # Escape single quotes in JAAS config for SQL
    sasl_jaas_config = producer_properties.get('sasl.jaas.config', '').replace("'", "''")

    # Create a Kafka table using Table API
    tbl_env.execute_sql(f"""
        CREATE TABLE kafka_flight_sink (
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
            'properties.bootstrap.servers' = '{producer_properties['bootstrap.servers']}',
            'format' = 'json',
            'properties.security.protocol' = '{producer_properties.get('security.protocol', 'PLAINTEXT')}',
            'properties.sasl.mechanism' = '{producer_properties.get('sasl.mechanism', '')}',
            'properties.sasl.jaas.config' = '{sasl_jaas_config}',
            'sink.delivery-guarantee' = 'exactly-once',
            'sink.transactional-id-prefix' = 'json-flight-data-'
        )
    """)
    
    # Create a StatementSet to execute multiple statements together
    logging.info("Created Kafka sink table")
    statement_set = tbl_env.create_statement_set()

    # Add the Iceberg table insert to the statement set
    statement_set.add_insert_sql(f"""
        INSERT INTO {flight_table_path.get_full_name()}
        SELECT * FROM temp_flight_view
    """)
    logging.info("Added Iceberg insert to statement set")
                                                                         
    # Add Kafka insert to the statement set
    statement_set.add_insert_sql("""
        INSERT INTO kafka_flight_sink
        SELECT * FROM temp_flight_view
    """)
    logging.info("Added Kafka insert to statement set")
    
    logging.info("Added Kafka insert to statement set")
    logging.info("Starting unified Flink job with both Iceberg and Kafka sinks")
    
    # Execute both inserts as ONE job
    try:
        statement_set.execute().wait()
    except Exception as e:
        logging.error("The App stopped early due to the following: %s", e)


def combine_datastreams(skyone_stream: DataStream, sunset_stream: DataStream) -> DataStream:
    """This function combines the SkyOne Airlines and Sunset Air flight data streams into
    a single data stream.

    Args:
        skyone_stream (DataStream): is the source of the SkyOne Airlines flight data.
        sunset_stream (DataStream): is the source of the Sunset Air flight data.

    Returns:
        DataStream: the union of the SkyOne Airlines and Sunset Air flight data streams.
    """
    from helper.common import parse_isoformat

    # Map the data streams to the FlightData model and filter out Skyone flights that have already arrived
    skyone_flight_stream = (skyone_stream
                            .map(lambda flight: AirlineFlightData.to_flight_data("SkyOne", flight))
                            .name("map_skyone_to_flight_data")
                            .uid("map_skyone_to_flight_data")
                            .filter(lambda flight: parse_isoformat(flight.arrival_time) > datetime.now(timezone.utc))
                            .name("filter_skyone_future_flights")
                            .uid("filter_skyone_future_flights"))
    

    # Map the data streams to the FlightData model and filter out Sunset flights that have already arrived
    sunset_flight_stream = (sunset_stream
                            .map(lambda flight: AirlineFlightData.to_flight_data("Sunset", flight))
                            .name("map_sunset_to_flight_data")
                            .uid("map_sunset_to_flight_data")
                            .filter(lambda flight: parse_isoformat(flight.arrival_time) > datetime.now(timezone.utc))
                            .name("filter_sunset_future_flights")
                            .uid("filter_sunset_future_flights"))
    
    # Return the union of the two data streams
    return skyone_flight_stream.union(sunset_flight_stream)


if __name__ == "__main__":
    main()