from pyflink.common import Row, WatermarkStrategy, Types
from pyflink.datastream.window import TumblingEventTimeWindows, Time
from pyflink.datastream import StreamExecutionEnvironment, DataStream, TimeCharacteristic
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer, DeliveryGuarantee
from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.table import DataTypes, StreamTableEnvironment
from pyflink.datastream.state import ValueStateDescriptor
from pyflink.table.expressions import col
from pyflink.datastream.functions import ProcessWindowFunction, RuntimeContext
from pyflink.table.udf import udtf, TableFunction
from typing import Iterator, Iterable
from datetime import datetime
import boto3
from botocore.exceptions import ClientError
import logging
import argparse
import json
from re import sub
import os
from dataclasses import dataclass
from decimal import Decimal

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


# Setup the logger
logger = logging.getLogger('UserStatisticsApp')

def serialize(obj):
    """
    This method serializes the `obj` parameter to a string.

    Args:
        obj (obj):  The object to serialize.

    Returns:
        str:  If the obj is of type datetime or date, the objec is formatted 
        according to ISO 8601 (i.e., 'YYYY-MM-DD HH:MM:SS.mmmmmm').  Otherwise, 
        the obj is returned as is.
    """
    if isinstance(obj, str):
        return obj
    return obj.isoformat(timespec="milliseconds")

@dataclass
class FlightData():
    email_address: str
    departure_time: str
    departure_airport_code: str
    arrival_time: str
    arrival_airport_code: str
    flight_number: str
    confirmation_code: str
    source: str


    def get_duration(self):
        return int((datetime.fromisoformat(self.arrival_time) - datetime.fromisoformat(self.departure_time)).seconds / 60)
    
    def to_row(self):
        return Row(email_address=self.email_address,
                   departure_time=serialize(self.departure_time),
                   departure_airport_code=self.departure_airport_code,
                   arrival_time=serialize(self.arrival_time),
                   arrival_airport_code=self.arrival_airport_code,
                   flight_number=self.flight_number,
                   confirmation_code=self.confirmation_code,
                   source=self.source)
    
    @classmethod
    def from_row(cls, row: Row):
        return cls(
            email_address=row.email_address,
            departure_time=row.departure_time,
            departure_airport_code=row.departure_airport_code,
            arrival_time=row.arrival_time,
            arrival_airport_code=row.arrival_airport_code,
            flight_number=row.flight_number,
            confirmation_code=row.confirmation_code,
            source=row.source,
        )

    @staticmethod
    def get_value_type_info():
        return Types.ROW_NAMED(
            field_names=[
                "email_address",
                "departure_time",
                "departure_airport_code",
                "arrival_time",
                "arrival_airport_code",
                "flight_number",
                "confirmation_code",
                "source",
            ],
            field_types=[
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
            ],
        )
    
    @staticmethod
    def to_user_statistics_data(row: Row):
        data = FlightData.from_row(row)
        return UserStatisticsData(data.email_address, data.get_duration(), 1)


@dataclass
class SkyOneAirlinesFlightData():
    email_address: str
    departure_time: str
    departure_airport_code: str
    arrival_time: str
    arrival_airport_code: str
    flight_number: str
    confirmation_code: str
    ticket_price: Decimal
    aircraft: str
    booking_agency_email: str

    
    @staticmethod
    def to_flight_data(row: Row):
        data = SkyOneAirlinesFlightData.from_row(row)
        return FlightData(
            data.email_address,
            data.departure_time,
            data.departure_airport_code,
            data.arrival_time,
            data.arrival_airport_code,
            data.flight_number,
            data.confirmation_code,
            "skyone",
        )
    
    @classmethod
    def from_row(cls, row: Row):
        return cls(email_address=row.email_address,
                   departure_time=row.departure_time,
                   departure_airport_code=row.departure_airport_code,
                   arrival_time=row.arrival_time,
                   arrival_airport_code=row.arrival_airport_code,
                   flight_number=row.flight_number,
                   confirmation_code=row.confirmation_code,
                   ticket_price=row.ticket_price,
                   aircraft=row.aircraft,
                   booking_agency_email=row.booking_agency_email)
    
    def to_row(self):
        return Row(email_address=self.email_address,
                   departure_time=serialize(self.departure_time),
                   departure_airport_code=self.departure_airport_code,
                   arrival_time=serialize(self.arrival_time),
                   arrival_airport_code=self.arrival_airport_code,
                   flight_number=self.flight_number,
                   confirmation_code=self.confirmation_code,
                   ticket_price=self.ticket_price,
                   aircraft=self.aircraft,
                   booking_agency_email=self.booking_agency_email)
    
    @staticmethod
    def get_value_type_info():
        return Types.ROW_NAMED(
            field_names=[
                "email_address",
                "departure_time",
                "departure_airport_code",
                "arrival_time",
                "arrival_airport_code",
                "flight_number",
                "confirmation_code",
                "ticket_price",
                "aircraft",
                "booking_agency_email",
            ],
            field_types=[
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
            ],
        )

@dataclass
class SunsetAirFlightData:
    email_address: str
    departure_time: str
    departure_airport_code: str
    arrival_time: str
    arrival_airport_code: str
    flight_number: str
    confirmation_code: str
    ticket_price: Decimal
    aircraft: str
    booking_agency_email: str

        
    @staticmethod
    def to_flight_data(row: Row):
        data = SunsetAirFlightData.from_row(row)
        return FlightData(
            data.email_address,
            data.departure_time,
            data.departure_airport_code,
            data.arrival_time,
            data.arrival_airport_code,
            data.flight_number,
            data.confirmation_code,
            "sunset",
        )
    
    @classmethod
    def from_row(cls, row: Row):
        return cls(email_address=row.email_address,
                   departure_time=row.departure_time,
                   departure_airport_code=row.departure_airport_code,
                   arrival_time=row.arrival_time,
                   arrival_airport_code=row.arrival_airport_code,
                   flight_number=row.flight_number,
                   confirmation_code=row.confirmation_code,
                   ticket_price=row.ticket_price,
                   aircraft=row.aircraft,
                   booking_agency_email=row.booking_agency_email)

    def to_row(self):
        return Row(email_address=self.email_address,
                   departure_time=serialize(self.departure_time),
                   departure_airport_code=self.departure_airport_code,
                   arrival_time=serialize(self.arrival_time),
                   arrival_airport_code=self.arrival_airport_code,
                   flight_number=self.flight_number,
                   confirmation_code=self.confirmation_code,
                   ticket_price=self.ticket_price,
                   aircraft=self.aircraft,
                   booking_agency_email=self.booking_agency_email)
    
    @staticmethod
    def get_value_type_info():
        return Types.ROW_NAMED(
            field_names=[
                "email_address",
                "departure_time",
                "departure_airport_code",
                "arrival_time",
                "arrival_airport_code",
                "flight_number",
                "confirmation_code",
                "ticket_price",
                "aircraft",
                "booking_agency_email",
            ],
            field_types=[
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
            ],
        )

@dataclass
class UserStatisticsData:
    email_address: str
    total_flight_duration: int
    number_of_flights: int

    def __init__(self, flight_data=None):
        if flight_data:
            self.email_address = flight_data.email_address
            self.total_flight_duration = flight_data.arrival_time - flight_data.departure_time
            self.number_of_flights = 1

    def merge(self, that):
        if self.email_address != that.email_address:
            raise ValueError("Cannot merge UserStatisticsData for different email addresses")

        merged = UserStatisticsData()
        merged.email_address = self.email_address
        merged.total_flight_duration = self.total_flight_duration + that.total_flight_duration
        merged.number_of_flights = self.number_of_flights + that.number_of_flights
        return merged

    def __eq__(self, other):
        if not isinstance(other, UserStatisticsData):
            return False
        return (self.email_address == other.email_address and
                self.total_flight_duration == other.total_flight_duration and
                self.number_of_flights == other.number_of_flights)

    def __hash__(self):
        return hash((self.email_address, self.total_flight_duration, self.number_of_flights))

    def __str__(self):
        return (f"UserStatisticsData{{"
                f"email_address='{self.email_address}', "
                f"total_flight_duration={self.total_flight_duration}, "
                f"number_of_flights={self.number_of_flights}}}")
    
    @classmethod
    def from_flight(cls, data: FlightData):
        return cls(
            email_address=data.email_address,
            total_flight_duration=data.get_duration(),
            number_of_flights=1,
        )

    @classmethod
    def from_row(cls, row: Row):
        return cls(
            email_address=row.email_address,
            total_flight_duration=row.total_flight_duration,
            number_of_flights=row.number_of_flights,
        )
    
    def to_row(self):
        return Row(email_address=self.email_address,
                   total_flight_duration=self.total_flight_duration,
                   number_of_flights=self.number_of_flights)
    
    @staticmethod
    def get_value_type_info():
        return Types.ROW_NAMED(
            field_names=[
                "email_address",
                "total_flight_duration",
                "number_of_flights",
            ],
            field_types=[
                Types.STRING(),
                Types.INT(),
                Types.INT(),
            ],
        )

class KafkaProperties(TableFunction):
    """This User-Defined Table Function (UDTF) is used to retrieve the Kafka Cluster properties
    from the AWS Secrets Manager and Parameter Store.
    """

    def __init__(self, for_consumer: bool, service_account_user: str):
        """Initializes the UDTF with the necessary parameters.

        Args:
            for_consumer (bool): determines if the Kafka Client is a consumer or producer.
            service_account_user (str): is the name of the service account user.  It is used in
            the prefix to the path of the Kafka Cluster secrets in the AWS Secrets Manager and
            the Kafka Client parameters in the AWS Systems Manager Parameter Store.
        """

        self._for_consumer = for_consumer
        self._service_account_user = service_account_user
        self._aws_region_name = os.environ['AWS_REGION']

    def eval(self, kakfa_properties: Row) -> Iterator[Row]:
        """This method retrieves the Kafka Cluster properties from the AWS Secrets Manager 
        and AWS Systems Manager.

        Args:
            kakfa_properties (Row): is a Row object that contains the Kafka Cluster properties.

        Raises:
            RuntimeError: is a generic error that is raised when an error occurs.

        Yields:
            Iterator[Row]: combination of Kafka Cluster properties and Kafka Client parameters.
        """

        # Get the Kafka Client properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
        secret_path_prefix = f"/confluent_cloud_resource/{self._service_account_user}"

        properties = self.get_kafka_properties(
            f"{secret_path_prefix}/kafka_cluster/java_client",
            f"{secret_path_prefix}/consumer_kafka_client" if self._for_consumer else f"{secret_path_prefix}/producer_kafka_client"
        )
        if properties is None:
            raise RuntimeError(f"Failed to retrieve the Kafka Client properties from '{secret_path_prefix}' secrets because {properties.get_error_message_code()}:{properties.get_error_message()}")
        else:
            for property_key, property_value in properties.items():
                yield Row(str(property_key), str(property_value))

    def get_kafka_properties(self, cluster_secrets_path: str, client_parameters_path: str) -> tuple[str, str]:
        """This method returns the Kafka Cluster properties from the AWS Secrets Manager and Parameter Store.

        Args:
            cluster_secrets_path (str): the path to the Kafka Cluster secrets in the AWS Secrets Manager.
            client_parameters_path (str): the path to the Kafka Client parameters in the AWS Systems Manager
            Parameter Store.

        Returns:
            properties (tuple[str, str]): the Kafka Cluster properties collection if successful, otherwise None.
        """
        
        properties = {}

        # Retrieve the SECRET properties from the AWS Secrets Manager
        secret = self.get_secrets(cluster_secrets_path)
        if secret is not None:
            try:
                # Convert the JSON object to a dictionary
                secret_data = secret
                for key in secret_data:
                    properties[key] = secret_data[key]

            except json.JSONDecodeError as e:
                return None

            # Retrieve the parameters from the AWS Systems Manager Parameter Store
            parameters = self.get_parameters(client_parameters_path)
            if parameters is not None:
                for key in parameters:
                    properties[key] = parameters[key]
                return properties
            else:
                return None
        else:
            return None
        
    def get_secrets(self, secrets_name: str) -> (dict):
        """This method retrieve secrets from the AWS Secrets Manager.
        
        Arg(s):
            secrets_name (str): Pass the name of the secrets you want the secrets for.
            
        Return(s):
            If successful, returns a JSON object of the secrets' value(s) stored.  Otherwise,
            the method has failed and 'None' is returned.
            
        Raise(s):
            DecryptionFailureException: Secrets Manager can't decrypt the protected secret text
            using the provided KMS key.
            InternalServiceErrorException: An internal server error exception object.
            InvalidParameterException: An input parameter violated a constraint.
            InvalidRequestException: Indicates that something is wrong with the input to the request.
            ResourceNotFoundExceptionAttributeError: The operation tried to access a keyspace or table
            that doesn't exist. The resource might not be specified correctly, or its status might not
            be ACTIVE.
        """
        
        # Create a Secrets Manager client
        session = boto3.session.Session()
        client = session.client(service_name='secretsmanager', region_name=self._aws_region_name)
        
        logging.info("AWS_ACCESS_KEY_ID: %s", os.environ['AWS_ACCESS_KEY_ID'])
        
        try:
            get_secret_value_response = client.get_secret_value(SecretId=secrets_name)
            
            # Decrypts secret using the associated KMS (Key Management System) CMK (Customer Master Key).
            return json.loads(get_secret_value_response['SecretString'])
        except ClientError as e:
            logging.error("Failed to get secrets (%s) from the AWS Secrets Manager because of %s.", secrets_name, e)
            if e.response['Error']['Code'] == 'DecryptionFailureException' or \
                e.response['Error']['Code'] == 'InternalServiceErrorException' or \
                e.response['Error']['Code'] == 'InvalidParameterException' or \
                e.response['Error']['Code'] == 'InvalidRequestException' or \
                e.response['Error']['Code'] == 'ResourceNotFoundException':
                    raise ValueError(e.response['Error']['Code'])
            return None

    def get_parameters(self, parameter_path: str) -> (dict):
        """This method retrieves the parameteres from the System Manager Parameter Store.
        Moreover, it converts the values to the appropriate data type.
        
        Arg(s):
            parameter_path (str): The hierarchy for the parameter.  Hierarchies start
            with a forward slash (/). The hierarchy is the parameter name except the last
            part of the parameter.  For the API call to succeed, the last part of the
            parameter name can't be in the path. A parameter name hierarchy can have a
            maximum of 15 levels.
            
        Return(s):
            parameters (dict): Goes throught recursively and returns all the parameters
            within a hierarchy.
        """
        
        session = boto3.session.Session()
        client = session.client(service_name='ssm', region_name=self._aws_region_name)
        
        try:
            response = client.get_parameters_by_path(Path=parameter_path, Recursive=False, WithDecryption=True)
        except ClientError as e:
            logging.error("Failed to get parameters from the AWS Systems Manager Parameter Store because of %s.", e)
            raise ValueError(e.response['Error']['Code'])
        else:
            parameters = {}
            for parameter in response['Parameters']:
                # Get the value of the parameter that will constitutes the key for the dictionary
                key = parameter['Name'][parameter['Name'].rfind('/') + 1:]
                
                # By default assume the parameter value is a string data type
                value = "" + parameter['Value'] + ""
                
                # Check if the value has zero decimal points, if so, maybe it's an integer
                # if not, go with the default string value
                if parameter['Value'].count('.') == 0:
                    try:
                        value = int(parameter['Value'].replace(',',''))
                    except Exception:
                        pass
                # Check if the value has only one decimal point, if so, maybe it's a float
                # if not, go with the default string value
                elif parameter['Value'].count('.') == 1:
                    try:
                        value = float(sub(r'[^\d.]', '', parameter['Value']))
                    except Exception:
                        pass
                    
                parameters[key] = value
                
            return parameters


def execute_kafka_properties_udtf(tbl_env, for_consumer: bool, service_account_user: str) -> dict:
    """This method retrieves the Kafka Cluster properties from the AWS Secrets Manager 
    and AWS Systems Manager.

    Args:
        tbl_env (TableEnvironment): is the instantiated Flink Table Environment.  A Table
        Environment is a unified entry point for creating Table and SQL API programs.  It
        is used to convert a DataStream into a Table or vice versa.  It is also used to
        register a Table in a catalog.
        for_consumer (bool): determines if the Kafka Client is a consumer or producer.
        service_account_user (str): is the name of the service account user.  It is used in
        the prefix to the path of the Kafka Cluster secrets in the AWS Secrets Manager and
        the Kafka Client parameters in the AWS Systems Manager Parameter Store.

    Returns:
        dict: combination of Kafka Cluster properties and Kafka Client parameters.
    """

    # Define the schema for the table and the return result of the UDTF
    schema = DataTypes.ROW([
        DataTypes.FIELD('property_key', DataTypes.STRING()),
        DataTypes.FIELD('property_value', DataTypes.STRING())
    ])

    # Create an empty table
    kafka_property_table = tbl_env.from_elements([('','')], schema)

    # Define the table name based on the type of Kafka client
    table_name = "kafka_property_table_" + ("consumer" if for_consumer else "producer")

    # Register the table as a temporary view
    tbl_env.create_temporary_view(table_name, kafka_property_table)

    # Get the table from the temporary view
    kafka_property_table = tbl_env.from_path(table_name)

    # print('\n Kafka Property Table Schema:--->')
    # kafka_property_table.print_schema()

    # Register the Python function as a PyFlink UDTF (User-Defined Table Function)
    kafka_properties_udtf = udtf(f=KafkaProperties(for_consumer, service_account_user), 
                                 result_types=schema)

    # Join the Kafka Property Table with the UDTF
    func_results = kafka_property_table.join_lateral(kafka_properties_udtf.alias("key", "value")).select(col("key"), col("value"))

    # print("\n Kafka " + ("Consumer" if for_consumer else "Producer") + " Client Property Table Data:--->")
    # func_results.execute().print()

    # Convert the result into a Python dictionary
    result = func_results.execute().collect()
    result_dict = {}
    for row in result:
        result_dict[row[0]] = row[1]
    result.close()

    # Convert the table back to a DataStream
    #result_stream = tbl_env.to_data_stream(kafka_property_table)
    
    # print('\n Kafka Client Properties Python dictionary:--->')
    # print(result_dict)

    # Return the table results into a dictionary
    return result_dict

class ProcessUserStatisticsDataFunction(ProcessWindowFunction):
    """This class is a custom implementation of a ProcessWindowFunction in Apache Flink.
    This class is designed to process elements within a window, manage state, and yield
    accumulated statistics for user data.
    """

    def __init__(self):
        """The purpose of this constructor is to set up the initial state of the instance by
        defining the state_descriptor attribute and initializing it to None.
        """

        self.state_descriptor = None

    def open(self, context: RuntimeContext):
        """The purpose of this open method is to set up the state descriptor that will be used
        later in the process method to manage state. By defining the state descriptor in the
        open method, the state is correctly initialized and available for use when processing
        elements.

        Args:
            context (RuntimeContext): An instance of the RuntimeContext class, which provides
            information about the runtime environment in which the function is executed.  This
            context can be used to access various runtime features, such as state, metrics, and
            configuration.
        """

        self.state_descriptor = ValueStateDescriptor("User Statistics Data", UserStatisticsData.get_value_type_info())

    def process(self, key: str, context: "ProcessWindowFunction.Context", elements: Iterable[UserStatisticsData]) -> Iterable:
        """This method processes elements within a window, updates the state, and yields
        the accumulated statistics.

        Args:
            key (str): Represents the key for the current group of elements.  This is useful
            for operations that need to be performed on a per-key basis.
            context (ProcessWindowFunction.Context): Provides access to the window's metadata,
            state, and timers.
            elements (Iterable[UserStatisticsData]): An iterable collection of `UserStatisticsData`
            objects that belong to the current window and key.  The method processes these elements
            to compute the result.

        Yields:
            Iterable: yeilds the accumulated statistics as a `UserStatisticsData` object.
        """

        # Retrieves the state associated with the current window
        state = context.global_state().get_state(self.state_descriptor)

        # Get's the current state
        accumulated_stats = state.value()

        # Iterates over the elements in the window
        for new_stats in elements:
            if accumulated_stats is None:
                # Initializes the first element's row representation
                accumulated_stats = new_stats.to_row()
            else:
                # Merges the current `accumulated_stats` with the new element's statistics
                # and updates the state
                accumulated_stats = UserStatisticsData.merge(UserStatisticsData.from_row(accumulated_stats), new_stats).to_row()

        # Updates the state with the new accumulated statistics
        state.update(accumulated_stats)

        # Yields the accumulated statistics as a `UserStatisticsData` object
        yield UserStatisticsData.from_row(accumulated_stats)


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
    (define_workflow(flight_data_stream)
     .map(lambda d: d.to_row(), output_type=UserStatisticsData.get_value_type_info())
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
