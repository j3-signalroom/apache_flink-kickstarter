from pyflink.common import Row, Types
from dataclasses import dataclass

from model.flight_data import FlightData

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


@dataclass
class UserStatisticsData:
    email_address: str
    total_flight_duration: int
    number_of_flights: int

    def __init__(self, flight_data=None):
        if flight_data:
            self.email_address = flight_data.email_address
            self.total_flight_duration = flight_data.arrival_time - flight_data.departure_time
            self.number_of_flights = 1

    def merge(self, that):
        if self.email_address != that.email_address:
            raise ValueError("Cannot merge UserStatisticsData for different email addresses")

        merged = UserStatisticsData()
        merged.email_address = self.email_address
        merged.total_flight_duration = self.total_flight_duration + that.total_flight_duration
        merged.number_of_flights = self.number_of_flights + that.number_of_flights
        return merged

    def __eq__(self, other):
        if not isinstance(other, UserStatisticsData):
            return False
        return (self.email_address == other.email_address and
                self.total_flight_duration == other.total_flight_duration and
                self.number_of_flights == other.number_of_flights)

    def __hash__(self):
        return hash((self.email_address, self.total_flight_duration, self.number_of_flights))

    def __str__(self):
        return (f"UserStatisticsData{{"
                f"email_address='{self.email_address}', "
                f"total_flight_duration={self.total_flight_duration}, "
                f"number_of_flights={self.number_of_flights}}}")
    
    @classmethod
    def from_flight(cls, data: FlightData):
        return cls(
            email_address=data.email_address,
            total_flight_duration=data.get_duration(),
            number_of_flights=1,
        )

    @classmethod
    def from_row(cls, row: Row):
        return cls(
            email_address=row.email_address,
            total_flight_duration=row.total_flight_duration,
            number_of_flights=row.number_of_flights,
        )
    
    def to_row(self):
        return Row(email_address=self.email_address,
                   total_flight_duration=self.total_flight_duration,
                   number_of_flights=self.number_of_flights)
    
    @staticmethod
    def get_value_type_info():
        return Types.ROW_NAMED(
            field_names=[
                "email_address",
                "total_flight_duration",
                "number_of_flights",
            ],
            field_types=[
                Types.STRING(),
                Types.INT(),
                Types.INT(),
            ],
        )
