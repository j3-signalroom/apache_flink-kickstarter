import logging
from confluent_kafka.schema_registry import SchemaRegistryClient, Schema
from contextlib import suppress

from aws_confluent_properties import AwsConfluentProperties

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


class SchemaRegistry:
    """Kafka Schema Registry class."""
    
    def __init__(self, service_account_user: str):
        """
        The initializor initializes all the class instance variable(s) with the argument(s) passed.
        Then retrieves the Schema Registry credentials from the AWS Secrets Manager, and stores the 
        credentials in the class instance.
        
        Arg(s):
            service_account_user (str): is the name of the service account user.  It is used in
            the prefix to the path of the Schema Registry Cluster secrets in the AWS Secrets Manager.
        """
        
        self._service_account_user = service_account_user
        
        #
        self.aws_service = AwsConfluentProperties()

        #
        schema_registry_path = f"/confluent_cloud_resource/{self._service_account_user}/schema_registry_cluster/python_client"
        self._schema_registry_properties = self.get_schema_registry_properties(schema_registry_path)
        if self._schema_registry_properties is None:
            raise RuntimeError(f"Failed to retrieve the Schema Registry properties from '{schema_registry_path}' secrets because {self._schema_registry_properties.get_error_message_code()}:{self._schema_registry_properties.get_error_message()}")
    
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
        self.client = SchemaRegistryClient(self._schema_registry_properties)
        
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
    