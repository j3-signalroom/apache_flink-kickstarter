import json
import requests
from requests.auth import HTTPBasicAuth
from pyflink.common.typeinfo import Types
from pyflink.common.serialization import DeserializationSchema
from pyflink.java_gateway import get_gateway, java_import
from pyflink.util.java_utils import load_java_class
from avro.io import DatumReader, BinaryDecoder
import avro.schema
import io


__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


class CustomAvroDeserializationSchema(DeserializationSchema):

    def __init__(self, schema_registry_properties: dict, topic_name: str, record_class: str):
        self._schema_registry_properties = schema_registry_properties
        self._topic_name = topic_name
        self._schema = None
        self._reader = None
        self.__fetch_avro_schema()

        gateway = get_gateway()
        java_import(gateway.jvm, record_class)
        j_record_class = load_java_class(record_class)
        JAvroRowDeserializationSchema = get_gateway().jvm.org.apache.flink.formats.avro.AvroRowDeserializationSchema
        j_deserialization_schema = JAvroRowDeserializationSchema(j_record_class)
        super(CustomAvroDeserializationSchema, self).__init__(j_deserialization_schema)

    def __fetch_avro_schema(self):
        """Fetch the latest Avro schema from the Confluent Schema Registry"""
        try:
            schema_registry_endpoint = f"{self._schema_registry_properties.get('schema.registry.url')}/subjects/{self._topic_name}-value/versions/latest"
            username = self._schema_registry_properties.get('schema.registry.basic.auth.user.info').split(":")[0]
            password = self._schema_registry_properties.get('schema.registry.basic.auth.user.info').split(":")[1]
            response = requests.get(schema_registry_endpoint, auth=HTTPBasicAuth(username, password))
            response.raise_for_status()

            schema_json = response.json()
            self._schema = avro.schema.parse(schema_json['schema'])
            self._reader = DatumReader(self._schema)

            print(f"Successfully fetched schema: {self._schema}")

        except requests.exceptions.RequestException as e:
            raise RuntimeError(f"Failed to fetch Avro schema from Schema Registry: {e}")

    def deserialize(self, message):
        try:
            # Create a BinaryDecoder and deserialize the message using DatumReader
            bytes_reader = io.BytesIO(message)
            decoder = BinaryDecoder(bytes_reader)
            record = self._reader.read(decoder)
            return json.dumps(record)  # Convert the record to JSON string if needed
        except Exception as e:
            # Handle the exception gracefully, e.g., log and skip the corrupted message
            print(f"Failed to deserialize Avro record: {e}")
            return None  # Skip the corrupted record

    def is_end_of_stream(self, next_element):
        return False

    def get_produced_type(self):
        # Define the produced type as a string (JSON format)
        return Types.STRING()