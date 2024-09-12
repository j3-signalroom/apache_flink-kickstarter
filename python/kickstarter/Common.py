__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@signalroom.ai"
__status__     = "dev"
#
# This class contains common methods that are used throughout the application.

import json
from typing import List
from datetime import datetime
from json import JSONDecoder, JSONEncoder
from functools import singledispatch
import logging

# Set up the logger
logger = logging.getLogger('Common')

OPT_SERVICE_ACCOUNT_USER = "--service-account-user"

def get_app_options(args: List[str]) -> str:
    """
    This method loops through the `args` parameter and checks for the `OPT_SERVICE_ACCOUNT_USER` option.

    :param args: List of strings passed to the main method.
    :return: Returns the service account user if the flag is found, an empty string otherwise.
    """
    service_account_user = ""

    # Loop through the args parameter and check for the `OPT_SERVICE_ACCOUNT_USER` option
    iterator = iter(args)
    for arg in iterator:
        if arg.lower() == OPT_SERVICE_ACCOUNT_USER.lower():
            service_account_user = next(iterator, "")

    return service_account_user


class CustomJSONEncoder(JSONEncoder):
    def default(self, obj):
        # Handling serialization of datetime objects
        if isinstance(obj, datetime):
            return obj.isoformat()
        # Let the base class default method raise the TypeError
        return super().default(obj)


def get_mapper():
    """
    Returns a new instance of the JSON encoder with custom handling for datetime serialization.

    :return: CustomJSONEncoder instance.
    """
    return CustomJSONEncoder()


if __name__ == "__main__":
    # Example usage for testing purposes
    example_args = ["--service-account-user", "example_user"]
    user = get_app_options(example_args)
    print(f"Service account user: {user}")
