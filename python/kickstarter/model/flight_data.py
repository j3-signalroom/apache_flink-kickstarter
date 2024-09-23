from dataclasses import dataclass
from datetime import datetime
import json
from common_functions import serialize
from pyflink.common import Types, Row

from model.user_statistics_data import UserStatisticsData

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


@dataclass
class FlightData:
    email_address: str | None
    departure_time: datetime | None
    departure_airport_code: str | None
    arrival_time: datetime | None
    arrival_airport_code: str | None
    flight_number: str | None
    confirmation_code: str | None

    def __eq__(self, other):
        if not isinstance(other, FlightData):
            return False
        return (self.email_address == other.email_address and
                self.departure_time == other.departure_time and
                self.departure_airport_code == other.departure_airport_code and
                self.arrival_time == other.arrival_time and
                self.arrival_airport_code == other.arrival_airport_code and
                self.flight_number == other.flight_number and
                self.confirmation_code == other.confirmation_code)

    def __hash__(self):
        return hash(self.email_address,
                    self.departure_time,
                    self.departure_airport_code,
                    self.arrival_time,
                    self.arrival_airport_code,
                    self.flight_number,
                    self.confirmation_code)

    def __str__(self):
        return json.dumps(self.__dict__, default=str)
    
    def get_duration(self):
        return int(( datetime.fromisoformat(self.arrival_time) - datetime.fromisoformat(self.departure_time)).seconds / 60)
    
    def to_row(self):
        return Row(
            email_address=self.email_address,
            departure_time=serialize(self.departure_time),
            departure_airport_code=self.departure_airport_code,
            arrival_time=serialize(self.arrival_time),
            arrival_airport_code=self.arrival_airport_code,
            flight_number=self.flight_number,
            confirmation=self.confirmation,
            source=self.source,
        )

    @classmethod
    def from_row(cls, row: Row):
        return cls(
            email_address=row.email_address,
            departure_time=row.departure_time,
            departure_airport_code=row.departure_airport_code,
            arrival_time=row.arrival_time,
            arrival_airport_code=row.arrival_airport_code,
            flight_number=row.flight_number,
            confirmation=row.confirmation,
            source=row.source,
        )

    @staticmethod
    def get_key_type_info():
        return Types.ROW_NAMED(
            field_names=[
                "confirmation",
            ],
            field_types=[
                Types.STRING(),
            ],
        )

    @staticmethod
    def get_value_type_info():
        return Types.ROW_NAMED(
            field_names=[
                "email_address",
                "departure_time",
                "departure_airport_code",
                "arrival_time",
                "arrival_airport_code",
                "flight_number",
                "confirmation",
                "source",
            ],
            field_types=[
                Types.STRING(),
                Types.SQL_TIMESTAMP(),
                Types.STRING(),
                Types.SQL_TIMESTAMP(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
            ],
        )

    @staticmethod
    def to_user_statistics_data(row: Row):
        data = FlightData.from_row(row)
        return UserStatisticsData(data.email_address, data.get_duration(), 1)
