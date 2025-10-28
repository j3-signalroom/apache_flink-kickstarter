from pyflink.common import Configuration, WatermarkStrategy
from pyflink.datastream.window import TumblingEventTimeWindows, Time
from pyflink.datastream import StreamExecutionEnvironment, DataStream
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer, DeliveryGuarantee
from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.datastream.checkpoint_config import ExternalizedCheckpointRetention
from pyflink.table import StreamTableEnvironment
from pyflink.table.catalog import ObjectPath
import logging
import os

from model.flight_data import FlightData, FlyerStatsData
from helper.flyer_stats_process_window_function import FlyerStatsProcessWindowFunction
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
    """The entry point to the Flyer Stats Flink App (a.k.a., Flink job graph --- DAG)."""
    # --- Retrieve environment variables
    service_account_user = os.getenv("SERVICE_ACCOUNT_USER", "")
    s3_bucket_name = os.getenv("AWS_S3_BUCKET_NAME", "")
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

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.flight`
    flight_source = (KafkaSource.builder()
                                .set_properties(consumer_properties)
                                .set_topics("airline.flight")
                                .set_group_id("flight_group")
                                .set_starting_offsets(KafkaOffsetsInitializer.earliest())
                                .set_value_only_deserializer(JsonRowDeserializationSchema
                                                             .builder()
                                                             .type_info(FlightData.get_value_type_info())
                                                             .build())
                                .build())

    # Takes the results of the Kafka source and attaches the unbounded data stream
    flight_data_stream = env.from_source(flight_source, WatermarkStrategy.for_monotonous_timestamps(), "flight_data_source")

    # Note: KafkaSink was introduced in Flink 1.14.0.  If you are using an older version of Flink, 
    # you will need to use the FlinkKafkaProducer class.
    # Initialize the KafkaSink builder
    kafka_sink_builder = KafkaSink.builder().set_bootstrap_servers(producer_properties['bootstrap.servers'])

    # Loop through the producer properties and set each property
    for key, value in producer_properties.items():
        if key != 'bootstrap.servers':  # Skip the bootstrap.servers as it is already set
            kafka_sink_builder.set_property(key, value)

    # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.flyer_stats`
    stats_sink = (kafka_sink_builder
                  .set_record_serializer(KafkaRecordSerializationSchema
                                         .builder()
                                         .set_topic("airline.flyer_stats")
                                         .set_value_serialization_schema(JsonRowSerializationSchema
                                                                         .builder()
                                                                         .with_type_info(FlyerStatsData.get_value_type_info())
                                                                         .build())
                                         .build())
                  .set_delivery_guarantee(DeliveryGuarantee.EXACTLY_ONCE)
                  .build())

    # --- Load Apache Iceberg catalog
    catalog = load_catalog(tbl_env, aws_region, s3_bucket_name, "apache_kickstarter")

    # --- Print the current catalog name
    print(f"Current catalog: {tbl_env.get_current_catalog()}")

    # --- Load database
    load_database(tbl_env, catalog, "airlines")
    
    # Print the current database name
    print(f"Current database: {tbl_env.get_current_database()}")

    # An ObjectPath in Apache Flink is a class that represents the fully qualified path to a
    # catalog object, such as a table, view, or function.  It uniquely identifies an object
    # within a catalog by encapsulating both the database name and the object name.  For 
    # instance, this case we using it to get the fully qualified path of the `flyer_stats`
    # table
    stats_table_path = ObjectPath(tbl_env.get_current_database(), "flyer_stats")

    # Check if the table exists.  If not, create it
    try:
        if not catalog.table_exists(stats_table_path):
            # Define the table using Flink SQL
            tbl_env.execute_sql(f"""
                CREATE TABLE {stats_table_path.get_full_name()} (
                    email_address STRING,
                    total_flight_duration INT,
                    number_of_flights INT
                    PARTITIONED BY (email_address)
                ) WITH (
                    'write.format.default' = 'parquet',
                    'write.target-file-size-bytes' = '134217728',
                    'write.delete.mode' = 'merge-on-read',
                    'write.update.mode' = 'merge-on-read',
                    'format-version' = '2'
                )
            """)
    except Exception as e:
        print(f"A critical error occurred to during the processing of the table because {e}")
        exit(1)

    # Define the workflow for the Flink job graph (DAG)
    stats_datastream = define_workflow(flight_data_stream).map(lambda d: d.to_row(), output_type=FlyerStatsData.get_value_type_info())

    # Populate the table with the data from the data stream
    (tbl_env.from_data_stream(stats_datastream)
            .execute_insert(stats_table_path.get_full_name()))

    # Sinks the User Statistics DataStream Kafka topic
    stats_datastream.sink_to(stats_sink).name("json_flyer_stats_sink").uid("json_flyer_stats_sink")

    # Execute the Flink job graph (DAG)
    try:
        env.execute("json_flyer_stats_app")
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
            .map(FlightData.to_flyer_stats_data)                                                # Transforms each element in the datastream to a FlyerStatsData object
            .name("map_to_flyer_stats_data")                                                    # Names the map operation for easier identification in the job graph
            .uid("map_to_flyer_stats_data")                                                     # Assigns a unique identifier to the map operation
            .key_by(lambda s: s.email_address)                                                  # Groups the data by email address
            .window(TumblingEventTimeWindows.of(Time.minutes(1)))                               # Each window will contain all events that occur within that 1-minute period
            .reduce(FlyerStatsData.merge, window_function=FlyerStatsProcessWindowFunction())    # Reduces the data within each window using the merge function and applies a custom window function
            .name("reduce_flyer_stats_data")                                                    # Names the reduce operation for easier identification in the job graph
            .uid("reduce_flyer_stats_data"))                                                    # Assigns a unique identifier to the reduce operation


if __name__ == "__main__":
    main()