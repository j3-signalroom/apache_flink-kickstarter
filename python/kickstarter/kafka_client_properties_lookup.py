from pyflink.common import Configuration
from pyflink.datastream.functions import MapFunction
from threading import Lock
from helper.kafka_client import KafkaClient
import threading

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


class KafkaClientPropertiesLookup(MapFunction):
    """
    An Apache Flink custom source data stream is a user-defined source of data that
    is integrated into a Flink application to read and process data from non-standard
    or custom sources. This custom source can be anything that isn't supported by Flink
    out of the box, such as proprietary REST APIs, specialized databases, custom hardware
    interfaces, etc. J3 utilizes a Custom Source Data Stream to read the AWS Secrets Manager
    secrets and AWS Systems Manager Parameter Store properties during the initial start of
    the Flink App, then caches the properties for use by any subsequent events that need
    these properties.

    Args:
        MapFunction (obj): In Apache Flink, the MapFunction class is a rich variant of the
        MapFunction class. It provides access to the RuntimeContext and includes setup and
        teardown methods. 
    """

    def __init__(self, consumer_kafka_client: bool, service_account_user: str):
        """
        Default constructor.
        
        :param consumer_kafka_client: A boolean indicating whether the Kafka client is a consumer or producer.
        :param service_account_user: The service account user.
        :raises Exception: If the service account user is empty.
        """
        if not service_account_user:
            raise Exception("The service account user must be provided.")

        # Set the class properties
        self._consumer_kafka_client = consumer_kafka_client
        self._service_account_user = service_account_user

        # Private attribute with a threading.Lock for atomic operations
        self._properties_lock = threading.Lock()
        self._properties = {}  # This acts like Java's Properties class

    def __getstate__(self):
        """
        This method is called when an object is pickled (a.k.a., serialized).

        Returns:
            dict:  It returns the object's state.
        """

        state = self.__dict__.copy()

        # Remove transient attributes from the state to be pickled
        del state['_properties']
        del state['_properties_lock']
        return state

    def __setstate__(self, state):
        """
        This method is called when an object is unpickled (a.k.a., deserialized). 
        It receives the state (the dictionary returned by __getstate__) and
        updates the object's __dict__.

        Args:
            state (dict):  The state of the object.
        """

        self.__dict__.update(state)

        # Restore transient attributes
        self._properties_lock = threading.Lock()
        self._properties = {}

    def open(self, configuration: Configuration):
        """
        This method is called once per parallel task instance when the job starts.
        It gets the Kafka Client properties from AWS Secrets Manager and
        AWS Systems Manager Parameter Store, then stores the properties in class attributes.
        
        :param configuration: The configuration containing the parameters attached to the contract.
        :raises Exception: Implementations may forward exceptions, which are caught by the runtime.
                           When the runtime catches an exception, it aborts the task and lets the
                           fail-over logic decide whether to retry the task execution.
        """
        # Get the Kafka Client properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
        secret_path_prefix = f"/confluent_cloud_resource/{self._service_account_user}"
        kafka_client = KafkaClient(
            f"{secret_path_prefix}/kafka_cluster/java_client",
            f"{secret_path_prefix}/consumer_kafka_client" if self._consumer_kafka_client else f"{secret_path_prefix}/producer_kafka_client"
        )
        properties = kafka_client.get_kafka_cluster_properties_from_aws()

        if not properties.is_successful():
            raise RuntimeError(f"Failed to retrieve the Kafka Client properties from '{secret_path_prefix}' secrets because {properties.get_error_message_code()}:{properties.get_error_message()}")
        else:
            # Set the class properties using thread-safe atomic operation
            with self._properties_lock:
                self._properties = properties.get()

    def map(self, value):
        """
        This method is called for each element of the input stream.
        
        :param value: The input value.
        :return: The result of the map operation.
        """
        with self._properties_lock:
            return self._properties

    def close(self):
        """
        This method is called when the task is canceled or the job is stopped.
        For this particular class, it is not used.
        
        :raises Exception: Implementations may forward exceptions, which are caught.
        """
        pass
