import logging
from contextlib import suppress
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka import SerializingProducer
from confluent_kafka.error import KeySerializationError
from confluent_kafka.error import ValueSerializationError
from confluent_kafka import KafkaException
import boto3
from botocore.exceptions import ClientError
import json
import sys
from datetime import datetime


class KafkaProducer:
    """Kafka Producer class."""
    
    def __init__(self, secrets_name: str, parameter_store_name: str):
        """
        The initializor initializes all the class instance variable(s) with the argument(s) passed.
        Then retrieves the Kafka cluster credentials from the AWS Secrets Manager, and stores the 
        credentials in the class instance.
        
        Arg(s):
            `secrets_name` (string): Pass the name of the secrets you want the secrets for.
            `parameter_store_name` (string): Pass the name of the parameter hierarchy.
        """
        
        self.secrets_name = secrets_name
        self.parameter_store_name = parameter_store_name
        
        # Get the Confluent Cloud envrionment Schema Registry credentials from the AWS Secrets Manager
        self.aws_service = AwsService()
        secrets = self.aws_service.get_secrets(secrets_name)
        
        if secrets is not None:
            self.cluster_server = secrets['bootstrap.servers']
            self.api_key = secrets['sasl.username']
            self.api_key_secret = secrets['sasl.password']
            self._initialized_successfully = True
        else:
            self._initialized_successfully = False
            
    @property
    def initialized_successfully(self):
        return self._initialized_successfully
                
    def load_topic(self, schema_registry_client: any, topic: str, avro_schema_key: any, avro_schema_value: any, dataframe: any) -> (bool):
        """
        This method publishes the Pandas dataframe into a Kafka Topic.
        
        Arg(s):
            `schema_registry_client` (any): Pass the Schema Registry Client object retrieving the Kafka Topics
            schemas from. 
            `topic` (string): Pass the name of the Kafka Topic you are publishing to.
            `avro_schema_key` (any): Pass the Avro formatted key schema.
            `avro_schema_value` (any): Pass the Avro formatted value schema.
            `dataframe` (any):  Pass the data content of the schemas in a Pandas dataframe.
            
        Return(s):
            If the method successfully produces events to the Kafka Topic, True is returned.  Otherwse, False
            is returned.
        """
        
        # Construct the Kafka cluster configuration dictionary
        kafka_config = {'bootstrap.servers': self.cluster_server, 
                        'sasl.username': self.api_key,
                        'sasl.password': self.api_key_secret}
        parameters = self.aws_service.get_parameter_values(self.parameter_store_name)
        for key, value in parameters.items():    
            kafka_config[key] = value
            
        # Serializes the Avro key and value schemas taken from the Confluent Schema Registry
        kafka_config['key.serializer'] = AvroSerializer(schema_registry_client = schema_registry_client, schema_str = avro_schema_key)
        kafka_config['value.serializer'] = AvroSerializer(schema_registry_client = schema_registry_client, schema_str =  avro_schema_value)
            
        # Create Producer instance
        producer = SerializingProducer(kafka_config)
        
        # Convert the key schema into a JSON object
        key_json_object = json.loads(avro_schema_key)
        
        # Set the dataframe to current timestamp
        dataframe['_row_kafka_load_at'] = str(datetime.now())
            
        # To sufficently increase the performance of the iteration.  The Pandas dataframe is 
        # converted into a dictionary
        dataframe_dict = dataframe.to_dict('records')
        for row in dataframe_dict:                
            # Fill-in the key
            key = {}
            for attribute in key_json_object['fields']:
                key[attribute.get('name')] = row[attribute.get('name')]
                
            def _delivery_report(err, msg):                
                """Delivery report callback for when a message is published succeeds or fails."""
                
                if err is not None:
                    logging.error('Failed to deliver message to %s Kafka topic because %s.', topic, err)
                    sys.exit(2)
                else:
                    logging.debug('Published message %s to %s Kafka topic at partition [%d] at offset %d.', key, msg.topic(), msg.partition(), msg.offset())
            
            # Publish messages to Kafka
            try:
                producer.produce(topic=topic, key=key, value=row, on_delivery=_delivery_report) 
            except BufferError as e:
                logging.error("Buffer Error --- %s:  %d number of messages behind.", e, len(producer))
                return False
            except KeySerializationError as e:
                logging.error("Key Serialization Error --- %s: key=%s", e, str(key))
                return False
            except ValueSerializationError as e:
                logging.error("Value Serialization Error --- %s: value=%s", e, str(value))
                return False
            except KafkaException as e: 
                logging.error('Kafka Publishing Error --- %s: key=%s value=%s.',  e, str(key), str(value))
                return False
                
            # Makes all buffered messages immediately available to send and blocks on the completion of the
            # requests associated with these records.
            producer.flush()
            
        return True
    
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

