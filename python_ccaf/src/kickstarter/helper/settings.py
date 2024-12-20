import os
import boto3
from botocore.exceptions import ClientError
import json
import logging
from typing import Dict

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


CC_PROPERTIES_PATHNAME = os.path.join(os.path.abspath(os.path.dirname(__file__)), "../../../config/cc.properties")

def get_secrets(self, aws_region_name: str, secrets_name: str) -> Dict[str, str]:
    """This method retrieve secrets from the AWS Secrets Manager.
    
    Arg(s):
        aws_region_name (str): Pass the AWS region name.
        secrets_name (str): Pass the name of the secrets you want the secrets for.
        
    Return(s):
        If successful, the secrets in a dict.  Otherwise, returns an empty dict.
    """
    client = self._session.client(service_name='secretsmanager', region_name=aws_region_name)        
    try:
        get_secret_value_response = client.get_secret_value(SecretId=secrets_name)
        return json.loads(get_secret_value_response['SecretString'])
    except ClientError as e:
        logging.error("Failed to get secrets (%s) from the AWS Secrets Manager because of %s.", secrets_name, e)
        return {}