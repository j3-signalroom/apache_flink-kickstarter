from pyflink.common import Configuration
from pyflink.datastream import SourceFunction
from threading import Lock

from helper.kafka_client import KafkaClient

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


class KafkaClientPropertiesLookup(SourceFunction):
    """
    An Apache Flink custom data source stream is a user-defined source of data that
    is integrated into a Flink application to read and process data from non-standard
    or custom sources. This custom source can be anything that isn't supported by Flink
    out of the box, such as proprietary REST APIs, specialized databases, custom hardware
    interfaces, etc. This code uses a Custom Data Source Stream to read the AWS Secrets 
    Manager secrets and AWS Systems Manager Parameter Store properties during the initial
    start of the Flink App, then caches the properties for use by any subsequent events
    that need these properties.

    Args:
        SourceFunction (obj): In Apache Flink, the SourceFunction class is a rich variant of the
        SourceFunction class. It provides access to the RuntimeContext and includes setup and
        teardown methods. 
    """

    def __init__(self, flag, options):
        self.flag = flag
        self.options = options
        
        if not options.get('s3_bucket_name'):
            raise Exception("The 's3_bucket_name' must be provided.")

        # Set the class properties
        self._is_consumer = options.get('is_consumer')
        self._service_account_user = options.get('s3_bucket_name')

        self._properties = {}  # This acts like Java's Properties class

        self.is_running = True

    def run(self, ctx):
        """
        This method is called once per parallel task instance when the job starts.
        It gets the Kafka Client properties from AWS Secrets Manager and
        AWS Systems Manager Parameter Store, then stores the properties in class attributes.        
        """

        # Get the Kafka Client properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
        secret_path_prefix = f"/confluent_cloud_resource/{self._service_account_user}"
        kafka_client = KafkaClient(
            f"{secret_path_prefix}/kafka_cluster/java_client",
            f"{secret_path_prefix}/consumer_kafka_client" if self._is_consumer else f"{secret_path_prefix}/producer_kafka_client"
        )
        properties = kafka_client.get_kafka_cluster_properties_from_aws()

        if not properties.is_successful():
            raise RuntimeError(f"Failed to retrieve the Kafka Client properties from '{secret_path_prefix}' secrets because {properties.get_error_message_code()}:{properties.get_error_message()}")
        else:
            ctx.collect(properties.get())
            self.is_running = False
    
    def cancel(self):
        self.is_running = False