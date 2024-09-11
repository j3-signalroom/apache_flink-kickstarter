import logging
from confluent_kafka import KafkaError
from confluent_kafka.admin import AdminClient, NewTopic
from dpg_coresaasservices.aws_connector import AwsService

        
class KafkaTopic:
    """Kafka Topics class."""
    
    def __init__(self, secrets_name: str):
        """
        The initializor initializes all the class instance variable(s) with the argument(s) passed.
        Then retrieves the Kafka Cluster credentials from the AWS Secrets Manager, and stores the 
        credentials in the class instance.
        
        Arg(s):
            `secrets_name` (string): Pass the name of the secrets you want the secrets for.
        """
        
        self.secrets_name = secrets_name
        
        # Get the Confluent Cloud envrionment Schema Registry credentials from the AWS Secrets Manager
        aws_service = AwsService()
        secrets = aws_service.get_secrets(secrets_name)
        
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

    def create_delete_if_exist_topic(self, topic_name: str):
        """
        This method deletes the Kafka Topic (if exist), and then creates a new one.
        
        Arg(s):
            topic_name (string): Pass the name of the Kafka Topic to be created.
            
        Return(s):
            If successful, returns the name back of the Kafka Topic created.  Otherwise,
            if during the creation process there is an error, an empty string is returned.
            
        Raise(s):
            Exception: If during the creation process, a Kafka error occurs.
        """
        
        # Connects to the Kakfa Cluster
        kafka_config = {'bootstrap.servers': self.cluster_server, 
                        'sasl.mechanism': 'PLAIN',
                        'security.protocol': 'SASL_SSL',
                        'sasl.username': self.api_key,
                        'sasl.password': self.api_key_secret}
        admin_client = AdminClient(kafka_config)
        
        """
        Asynchronously delete topic.  By default this operation on the broker (the cluster in this
        case since we are using Confluent Cloud) returns immediately while a topic is deleted in the 
        background.  So, we will give it some time like (5s) to propagate in the cluster before 
        returning.  The method returns a dict of <topic,future_result>
        """
        future_result = admin_client.delete_topics([topic_name], operation_timeout=5)

        # Wait for operation to finish.
        for topic, future in future_result.items():
            try:
                future.result()  # The result itself is None
                logging.info("Topic %s deleted.", topic)
            except Exception as e:
                # Continue if error code is UNKNOWN_TOPIC_OR_PART, because the topic was never created before
                if e.args[0].code() != KafkaError.UNKNOWN_TOPIC_OR_PART:
                    message = 'Failed to delete topic {topic}: {error_code} - {error_message}.'.format(topic=topic, error_code=e.args[0].code(), error_message=e)
                    logging.error(message)
                    raise ValueError(message)
        
        """
        Asynchronously creates topic.  By default this operation on the broker (the cluster in this
        case since we are using Confluent Cloud) returns immediately while a topic is created in the 
        background.  So, we will give it some time like (5s) to propagate in the cluster before 
        returning.  The method returns a dict of <topic,future_result>
        """
        while True:
            future_result = admin_client.create_topics([NewTopic(topic_name, num_partitions=1, replication_factor=3)], operation_timeout=5)
            for topic, future in future_result.items():
                try:
                    future.result()  # The result itself is None
                    logging.info('Topic %s created.', topic)
                    return str(topic)
                except Exception as e:
                    # Continue if error code is TOPIC_ALREADY_EXISTS, because the topic was dropped yet
                    if e.args[0].code() != KafkaError.TOPIC_ALREADY_EXISTS:
                        message = 'Failed to create topic {topic}: {error_code} - {error_message}.'.format(topic=topic, error_code=e.args[0].code(), error_message=e)
                        logging.error(message)
                        raise ValueError(message)
        return str('')
    