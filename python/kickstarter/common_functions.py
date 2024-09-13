from typing import List
from functools import singledispatch

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


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
