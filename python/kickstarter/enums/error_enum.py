# Copyright (c) 2024 Jeffrey Jonathan Jennings
# Author: Jeffrey Jonathan Jennings (J3)

from enum import Enum


class error_enum(Enum):
    # Enumerators with their error codes and messages
    ERR_CODE_MISSING_OR_INVALID_FIELD = ("ERR_CODE_MISSING_OR_INVALID_FIELD", "")
    ERR_CODE_IO_EXCEPTION = ("ERR_CODE_IO_EXCEPTION", "")
    ERR_CODE_AWS_REQUESTED_SECRET_DOES_HAVE_NOT_STRING = ("ERR_CODE_AWS_REQUESTED_SECRET_DOES_HAVE_NOT_STRING", "")
    ERR_CODE_AWS_REQUESTED_SECRET_HAS_NO_VALUE = ("ERR_CODE_AWS_REQUESTED_SECRET_HAS_NO_VALUE", "")
    ERR_CODE_AWS_REQUESTED_SECRET_NOT_FOUND = ("ERR_CODE_AWS_REQUESTED_SECRET_NOT_FOUND", "")
    ERR_CODE_AWS_INVALID_REQUEST = ("ERR_CODE_AWS_INVALID_REQUEST", "")
    ERR_CODE_AWS_INVALID_PARAMETERS = ("ERR_CODE_AWS_INVALID_PARAMETERS", "")
    ERR_CODE_AWS_REQUESTED_PARAMETER_PREFIX_NOT_FOUND = ("ERR_CODE_AWS_REQUESTED_PARAMETER_PREFIX_NOT_FOUND", "")

    _BY_ERROR_CODE = {}
    _BY_ERROR_MESSAGE = {}

    def __init__(self, error_message_code, error_message):
        self.error_message_code = error_message_code
        self.error_message = error_message

    @classmethod
    def initialize_mappings(cls):
        """
        Initializes the mappings for quick lookup by error code and message.
        """
        for error in cls:
            cls._BY_ERROR_CODE[error.error_message_code] = error
            cls._BY_ERROR_MESSAGE[error.error_message] = error

    @classmethod
    def value_of_error_message_code(cls, error_message_code):
        """
        This method returns the enum value by the error_message_code.

        :param error_message_code: The error code.
        :return: The corresponding enum value.
        """
        return cls._BY_ERROR_CODE.get(error_message_code)

    @classmethod
    def value_of_error_message(cls, error_message):
        """
        This method returns the enum value by the error_message.

        :param error_message: The error message.
        :return: The corresponding enum value.
        """
        return cls._BY_ERROR_MESSAGE.get(error_message)

    def get_code(self):
        """
        :return: The error code.
        """
        return self.error_message_code

    def get_message(self):
        """
        :return: The error message.
        """
        return self.error_message


# Initialize mappings
error_enum.initialize_mappings()

# Example usage for testing purposes
if __name__ == "__main__":
    error = error_enum.value_of_error_message_code("ERR_CODE_IO_EXCEPTION")
    print(f"Error Code: {error.get_code() if error else 'Not Found'}, Message: {error.get_message() if error else 'Not Found'}")
