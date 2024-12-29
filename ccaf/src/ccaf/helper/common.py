import os
import boto3
from botocore.exceptions import ClientError
import json
import logging
from typing import Dict
from pyflink.table.confluent import ConfluentSettings


__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


CC_PROPERTIES_PATHNAME = os.path.join(os.path.abspath(os.path.dirname(__file__)), "../../../config/cc.properties")

# Confluent Cloud for Apache Flink Secrets Keys
ENVIRONMENT_ID = "environment.id"
FLINK_API_KEY = "flink.api.key"
FLINK_API_SECRET = "flink.api.secret"
FLINK_CLOUD = "flink.cloud"
FLINK_COMPUTE_POOL_ID = "flink.compute.pool.id"
FLINK_PRINCIPAL_ID = "flink.principal.id"
FLINK_REGION = "flink.region"
ORGANIZATION_ID = "organization.id"


def get_secrets(aws_region: str, secrets_name: str) -> Dict[str, str]:
    """This method retrieve secrets from the AWS Secrets Manager.
    
    Arg(s):
        aws_region (str):    The AWS region.
        secrets_name (str):  The AWS Secrets Manager secret name.
        
    Return(s):
        If successful, the secrets in a dict.  Otherwise, returns an empty dict.
    """
    session = boto3.session.Session()
    client = session.client(service_name='secretsmanager', region_name=aws_region)        
    try:
        get_secret_value_response = client.get_secret_value(SecretId=secrets_name)
        return json.loads(get_secret_value_response['SecretString'])
    except ClientError as e:
        logging.error("Failed to get secrets (%s) from the AWS Secrets Manager because of %s.", secrets_name, e)
        return {}


def get_cc_properties(aws_region: str, secrets_name: str) -> ConfluentSettings:
    """
    This method retrieves the Confluent Cloud for Apache Flink properties from the AWS Secrets Manager.

    Args:
        aws_region (str):    The AWS region.
        secrets_name (str):  The AWS Secrets Manager secret name.
        

    Returns:
        ConfluentSettings: The Confluent Cloud for Apache Flink settings.
    """
    settings = get_secrets(aws_region, secrets_name)

    # Build the ConfluentSettings object.
    return (
        ConfluentSettings
            .new_builder()
            .set_cloud(settings[FLINK_CLOUD])
            .set_region(settings[FLINK_REGION])
            .set_flink_api_key(settings[FLINK_API_KEY])
            .set_flink_api_secret(settings[FLINK_API_SECRET])
            .set_organization_id(settings[ORGANIZATION_ID])
            .set_environment_id(settings[ENVIRONMENT_ID])
            .set_compute_pool_id(settings[FLINK_COMPUTE_POOL_ID])
            .build()
    )