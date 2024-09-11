import logging
from contextlib import suppress
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka import SerializingProducer
from confluent_kafka.error import KeySerializationError
from confluent_kafka.error import ValueSerializationError
from confluent_kafka import KafkaException
import json
import sys
from datetime import datetime
from aws_connector import aws_service as AwsService


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
