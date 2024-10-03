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
        str:  If the obj is of type datetime or date, the objec is formatted 
        according to ISO 8601 (i.e., 'YYYY-MM-DD HH:MM:SS.mmmmmm').  Otherwise, 
        the obj is returned as is.
    """
    if isinstance(obj, str):
        return obj
    return obj.isoformat(timespec="milliseconds")
