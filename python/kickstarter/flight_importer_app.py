from pyflink.common import Row, WatermarkStrategy, Types
from pyflink.datastream import StreamExecutionEnvironment, DataStream
from pyflink.datastream.connectors.kafka import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, KafkaOffsetsInitializer, DeliveryGuarantee
from pyflink.datastream.formats.json import JsonRowDeserializationSchema, JsonRowSerializationSchema
from pyflink.table import DataTypes, StreamTableEnvironment
from pyflink.table.expressions import col
from pyflink.table.udf import udtf, TableFunction
from typing import Iterator
from datetime import datetime, timedelta, timezone
import boto3
from botocore.exceptions import ClientError
import logging
import argparse
import json
from re import sub
import os
from dataclasses import dataclass
from decimal import Decimal
from typing import Optional

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


# Setup the logger
logger = logging.getLogger('FlightImporterApp')

def serialize(obj):
    """
    This method serializes the `obj` parameter to a string.

    Args:
        obj (obj):  The object to serialize.

    Returns:
        str:  If the obj is a datetime object, the time formatted according to 
        ISO is returned (i.e., 'YYYY-MM-DD HH:MM:SS.mmmmmm').  If the obj is a 
        date object, the date is returned. Otherwise, the obj is returned as is.
    """
    if isinstance(obj, datetime.date):
        return obj.isoformat()
    return obj

@dataclass
class FlightData():
    email_address: str | None
    departure_time: Types.SQL_TIMESTAMP
    departure_airport_code: str | None
    arrival_time: Types.SQL_TIMESTAMP
    arrival_airport_code: str | None
    flight_number: str | None
    confirmation_code: str | None
    source: str | None


    def get_duration(self):
        return int((self.arrival_time - self.departure_time).seconds / 60)
    
    def to_row(self):
        return {
            'email_address': self.email_address,
            'departure_time': serialize(self.departure_time),
            'departure_airport_code': self.departure_airport_code,
            'arrival_time': serialize(self.arrival_time),
            'arrival_airport_code': self.arrival_airport_code,
            'flight_number': self.flight_number,
            'confirmation_code': self.confirmation_code,
            'source': self.source,
        }
    
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

@dataclass
class SkyOneAirlinesFlightData():
    email_address: str | None
    departure_time: Types.SQL_TIMESTAMP
    departure_airport_code: str | None
    arrival_time: Types.SQL_TIMESTAMP
    arrival_airport_code: str | None
    flight_number: str | None
    confirmation_code: str | None
    ticket_price: Decimal | None
    aircraft: str | None
    booking_agency_email: str | None

    
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
        return cls(email_address=row.email_address or "",
                   departure_time=row.departure_time or None,
                   departure_airport_code=row.departure_airport_code or "",
                   arrival_time=row.arrival_time or None,
                   arrival_airport_code=row.arrival_airport_code or "",
                   flight_number=row.flight_number or "",
                   confirmation_code=row.confirmation_code or "",
                   ticket_price=row.ticket_price or 0,
                   aircraft=row.aircraft or "",
                   booking_agency_email=row.booking_agency_email or "")
    
    def to_row(self):
        return Row(email_address=self.email_address or "",
                   departure_time=serialize(self.departure_time or None),
                   departure_airport_code=self.departure_airport_code or "",
                   arrival_time=serialize(self.arrival_time or None),
                   arrival_airport_code=self.arrival_airport_code or "",
                   flight_number=self.flight_number or "",
                   confirmation_code=self.confirmation_code or "",
                   ticket_price=self.ticket_price or 0,
                   aircraft=self.aircraft or "",
                   booking_agency_email=self.booking_agency_email or "")
    
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
    email_address: str | None
    departure_time: Types.SQL_TIMESTAMP
    departure_airport_code: str | None
    arrival_time: Types.SQL_TIMESTAMP
    arrival_airport_code: str | None
    flight_number: str | None
    confirmation_code: str | None
    ticket_price: Decimal | None
    aircraft: str | None
    booking_agency_email: str | None

        
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
        return cls(email_address=row.email_address or "",
                   departure_time=row.departure_time or None,
                   departure_airport_code=row.departure_airport_code or "",
                   arrival_time=row.arrival_time or None,
                   arrival_airport_code=row.arrival_airport_code or "",
                   flight_number=row.flight_number or "",
                   confirmation_code=row.confirmation_code or "",
                   ticket_price=row.ticket_price or 0,
                   aircraft=row.aircraft or "",
                   booking_agency_email=row.booking_agency_email or "")

    def to_row(self):
        return Row(email_address=self.email_address or "",
                   departure_time=serialize(self.departure_time or None),
                   departure_airport_code=self.departure_airport_code or "",
                   arrival_time=serialize(self.arrival_time or None),
                   arrival_airport_code=self.arrival_airport_code or "",
                   flight_number=self.flight_number or "",
                   confirmation_code=self.confirmation_code or "",
                   ticket_price=self.ticket_price or 0,
                   aircraft=self.aircraft or "",
                   booking_agency_email=self.booking_agency_email or "")
    
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

    def get_kafka_properties(self, cluster_secrets_path: str, client_parameters_path: str) -> tuple[str, str | None]:
        """This method returns the Kafka Cluster properties from the AWS Secrets Manager and Parameter Store.

        Args:
            cluster_secrets_path (str): the path to the Kafka Cluster secrets in the AWS Secrets Manager.
            client_parameters_path (str): the path to the Kafka Client parameters in the AWS Systems Manager
            Parameter Store.

        Returns:
            properties (tuple[str, str | None]): the Kafka Cluster properties collection if successful, otherwise None.
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

def main(args):
    """This is the main function that sets up the Flink job graph (DAG) for the Flight Importer App.
        
    Args:
        args (argparse.Namespace): is the arguments passed to the script.
    """

    # Create a blank Flink execution environment
    env = StreamExecutionEnvironment.get_execution_environment()

    # Create a Table Environment
    tbl_env = StreamTableEnvironment.create(stream_execution_environment=env)

    # Adjust resource configuration
    env.set_parallelism(1)  # Set parallelism to 1 for simplicity

    # Get the Kafka Cluster properties for the consumer
    consumer_properties = execute_kafka_properties_udtf(tbl_env, True, args.s3_bucket_name)

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.skyone`
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
    skyone_stream = env.from_source(skyone_source, WatermarkStrategy.no_watermarks(), "skyone_source")

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
    sunset_stream = env.from_source(sunset_source, WatermarkStrategy.no_watermarks(), "sunset_source")

    # Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.all`
    # Get the Kafka Cluster properties for the producer
    producer_properties = execute_kafka_properties_udtf(tbl_env, False, args.s3_bucket_name)
    producer_properties.update({
        'transaction.timeout.ms': '60000'  # Set transaction timeout to 60 seconds
    })
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

    # Defines the workflow for the Flink job graph (DAG) by connecting the data streams
    (define_workflow(skyone_stream, sunset_stream)
     .map(lambda d: d.to_row(), output_type=FlightData.get_value_type_info())
     .sink_to(flight_sink)
     .name("flightdata_sink"))

    try:
        env.execute("FlightImporterApp")
    except Exception as e:
        logger.error("The App stopped early due to the following: %s", e)

def define_workflow(skyone_stream: DataStream, sunset_stream: DataStream) -> DataStream:
    """This method defines the workflow for the Flink job graph (DAG) by connecting the data streams.

    Args:
        skyone_stream (DataStream): is the source of the SkyOne Airlines flight data.
        sunset_stream (DataStream): is the source of the Sunset Air flight data.

    Returns:
        DataStream: the union of the SkyOne Airlines and Sunset Air flight data streams.
    """

    def to_aware_datetime(dt):
        if isinstance(dt, str):
            dt = datetime.datetime.strptime(dt, "%Y-%m-%dT%H:%M:%S.%f%z")
        if dt.tzinfo is None:
            return dt.replace(tzinfo=timezone.utc)
        return dt
    
    skyone_flight_stream = (skyone_stream
                            .map(SkyOneAirlinesFlightData.to_flight_data)
                            .filter(lambda flight: flight.arrival_time is not None and to_aware_datetime(flight.arrival_time) > datetime.now(timezone.utc)))

    sunset_flight_stream = (sunset_stream
                            .map(SunsetAirFlightData.to_flight_data)
                            .filter(lambda flight: flight.arrival_time is not None and to_aware_datetime(flight.arrival_time) > datetime.now(timezone.utc)))
    
    # Return the union of the SkyOne Airlines and Sunset Air flight data streams
    # or the SkyOne Airlines flight data stream if the Sunset Air flight data stream is empty
    # or the Sunset Air flight data stream if the SkyOne Airlines flight data stream is empty
    if skyone_flight_stream and sunset_flight_stream:
        return skyone_flight_stream.union(sunset_flight_stream)
    elif skyone_flight_stream:
        return skyone_flight_stream
    elif sunset_flight_stream:
        return sunset_flight_stream

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--aws_s3_bucket',
        dest='s3_bucket_name',
        required=True,
        help='The AWS S3 bucket name.')
    known_args, _ = parser.parse_known_args()
    main(known_args)
