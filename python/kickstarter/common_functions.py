from typing import List
import datetime
from json import JSONEncoder

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def serialize(obj):
    """
    This method serializes the `obj` parameter to a string.

    Args:
        obj (obj):  The object to serialize.

    Returns:
        str:  If the obj is a datetime object, the time formatted according to 
        ISO is returned (i.e., 'YYYY-MM-DD HH:MM:SS.mmmmmm').  If the obj is a 
        date object, the date is returned. Otherwise, the obj is returned as is.
    """
    if isinstance(obj, datetime.datetime):
        return obj.isoformat(timespec="milliseconds")
    if isinstance(obj, datetime.date):
        return str(obj)
    return obj
