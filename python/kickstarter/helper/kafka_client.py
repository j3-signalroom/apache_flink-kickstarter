import json
from enums import ErrorEnum
from helper import AwsHelper

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


class KafkaClient:
    def __init__(self, kafka_cluster_secrets_path: str, kafka_client_parameters_path: str):
        """
        The default constructor stores the parameter values that are passed to it.
        
        :param kafka_cluster_secrets_path: The path to the Kafka Cluster secrets in AWS Secrets Manager.
        :param kafka_client_parameters_path: The path to the Kafka Client parameters in AWS Systems Manager Parameter Store.
        """
        self.kafka_cluster_secrets_path = kafka_cluster_secrets_path
        self.kafka_client_parameters_path = kafka_client_parameters_path

    def get_kafka_cluster_properties_from_aws(self) -> tuple[str, str | None]:
        """
        This method returns the Kafka Cluster properties from the AWS Secrets Manager and Parameter Store.

        :return: An ObjectResult containing the Kafka Cluster properties collection.
        """
        properties = {}

        # Retrieve the SECRET properties from the AWS Secrets Manager
        secret = AwsHelper.get_secrets(self.kafka_cluster_secrets_path, "AWSCURRENT")
        if secret.is_successful():
            try:
                # Convert the JSON object to a dictionary
                secret_data = secret.get()
                for key in secret_data:
                    properties[key] = secret_data[key]

            except json.JSONDecodeError as e:
                return ErrorEnum.ERR_CODE_MISSING_OR_INVALID_FIELD.get_code(), str(e)

            # Retrieve the parameters from the AWS Systems Manager Parameter Store
            parameters = AwsHelper.get_parameters(self.kafka_client_parameters_path)
            if parameters.is_successful():
                return {**properties, **parameters.get()}
            else:
                return parameters.get_error_message_code(), parameters.get_error_message()

        else:
            return secret.get_error_message_code(), secret.get_error_message()

