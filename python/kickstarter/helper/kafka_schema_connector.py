import logging
from confluent_kafka.schema_registry import SchemaRegistryClient, Schema
from confluent_kafka.schema_registry.error import SchemaRegistryError
from aws_connector import aws_service as AwsService
from contextlib import suppress


class SchemaRegistry:
    """Kafka Schema Registry class."""
    
    def __init__(self, secrets_name: str):
        """
        The initializor initializes all the class instance variable(s) with the argument(s) passed.
        Then retrieves the Schema Registry credentials from the AWS Secrets Manager, and stores the 
        credentials in the class instance.
        
        Arg(s):
            `secrets_name` (string): Pass the name of the secrets you want the secrets for.
        """
        
        self.secrets_name = secrets_name
        
         # Get the Confluent Cloud envrionment Schema Registry credentials from the AWS Secrets Manager
        aws_service = AwsService()
        secrets = aws_service.get_secrets(secrets_name)
        
        if secrets is not None:
            self.url = secrets["schema.registry.url"]
            self.username_password = secrets["schema.registry.basic.auth.user.info"]
            self.client = None
            self._initialized_successfully = True
        else:
            self._initialized_successfully = False
        
    @property
    def initialized_successfully(self):
        return self._initialized_successfully
    
    def create_delete_if_exist_schema(self, subject_name: str, schema_type: str, schema_string: str) -> (any):
        """
        This method deletes schema (and all its versions), if exist.  Then Takes the `schema_string`, 
        saves it as the `schema_type`, and puts it in in the Schema Registry as a new entry.
        
        Arg(s):
            `subject_name` (string): Pass the subject name of the Schema.
            `schema_type` (string): Pass the type of Schema it is (i.e., Key or Value).
            `schema_string` (string): Pass the JSON Object of the Schema represented as a String.
        
        Return(s):
            schema_id (integer): Returns the Schema Version ID that is returned by the Schema
            Registry.
        """
        
        # Connect to the Schema Registry
        schema_registry_config = {'url': self.url, 'basic.auth.user.info': self.username_password}
        self.client = SchemaRegistryClient(schema_registry_config)
        
        # Delete schema and all versions of it
        with suppress(Exception):
            self.client.delete_subject(subject_name, True)
            
        # Converts schema into a byte array for transport over the wire
        schema = Schema(schema_string, schema_type)
        
        # Registers the schema with the Schema Registry
        schema_id = self.client.register_schema(subject_name, schema)
        logging.info('%s Avro Schema ID: %d', subject_name, schema_id)
        
        return schema_id
    
    def get_schema_registry_client(self):
        """Get the Schema Registry Client."""
        
        return self.client
    