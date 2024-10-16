from pyflink.datastream import StreamExecutionEnvironment
from datetime import datetime, timezone


__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def serialize_date(obj):
    """
    This method serializes the `obj` parameter to a string.

    Args:
        obj (obj):  The object to serialize, if type datetime or date.

    Returns:
        str:  If the obj is of type datetime or date, the object is formatted 
        according to ISO 8601 (i.e., 'YYYY-MM-DD HH:MM:SS').  Otherwise, 
        the obj is returned as is.
    """
    if isinstance(obj, str):
        return obj
    return obj.isoformat(timespec="milliseconds")

def parse_isoformat(date_string: str) -> datetime:
    """This method parses a string representing a date and time in ISO 8601 format.

    Args:
        date_string (str): The string representing a date and time in ISO 8601 format.

    Returns:
        datetime: The datetime object representing the date and time in ISO 8601 format.
    """
    try:
        date_time_obj = datetime.strptime(date_string, "%Y-%m-%d %H:%M:%S")
        date_time_utc = date_time_obj.replace(tzinfo=timezone.utc).astimezone(timezone.utc)
        return date_time_utc
    except ValueError:
        print(f"Invalid isoformat string: '{date_string}'")
        return None
    
def catalog_exist(tbl_env: StreamExecutionEnvironment, catalog_to_check: str) -> bool:
    """This method checks if the catalog exist in the environment.

    Args:
        tbl_env (StreamExecutionEnvironment): The StreamExecutionEnvironment is the context
        in which a streaming program is executed. 
        catalog_to_check (str): The name of the catalog to be checked if its name exist in the
        environment.

    Returns:
        bool: True is returned, if the catalog exist in the environment.  Otherwise, False is
        returned.
    """
    catalogs = tbl_env.list_catalogs()

    # Check if a specific catalog exists
    if catalog_to_check in catalogs:
        return True
    else:
        return False