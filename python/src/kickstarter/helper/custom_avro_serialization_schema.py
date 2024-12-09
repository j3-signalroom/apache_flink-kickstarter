from pyflink.common.typeinfo import Types
from pyflink.common.serialization import SerializationSchema
from pyflink.java_gateway import get_gateway, java_import
from pyflink.util.java_utils import load_java_class
import io
from avro.io import BinaryEncoder


__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


class CustomAvroSerializationSchema(SerializationSchema):
    def __init__(self, record_class: str):
        gateway = get_gateway()
        java_import(gateway.jvm, record_class)
        j_record_class = load_java_class(record_class)
        JAvroRowSerializationSchema = get_gateway().jvm.org.apache.flink.formats.avro.AvroRowSerializationSchema
        j_serialization_schema = JAvroRowSerializationSchema(j_record_class)
        super(CustomAvroSerializationSchema, self).__init__(j_serialization_schema)

    def serialize(self, element):
        """
        Serializes the Python dictionary to Avro bytes.

        :param element: A dictionary representing the data to serialize.
        :return: The serialized Avro record in byte format.
        """
        try:
            # Create a byte buffer to store serialized bytes
            bytes_writer = io.BytesIO()
            encoder = BinaryEncoder(bytes_writer)
            
            # Write the data element to Avro using DatumWriter
            self.writer.write(element, encoder)
            
            # Get the byte value
            return bytes_writer.getvalue()
        except Exception as e:
            # Log error or handle exception
            print(f"Error in serializing element {element}: {e}")
            return None  # Optionally handle serialization error more robustly

    def get_produced_type(self):
        """
        Returns the produced data type of the serialized elements.
        """
        return Types.PICKLED_BYTE_ARRAY()
