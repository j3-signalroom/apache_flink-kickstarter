from pyflink.common import Row, Types
from dataclasses import dataclass

from helper.common import serialize_date, parse_isoformat

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


@dataclass
class FlightData():
    email_address: str | None
    departure_time: str | None
    departure_airport_code: str | None
    arrival_time: str | None
    arrival_airport_code: str | None
    flight_number: str | None
    confirmation_code: str | None
    airline: str | None


    def get_duration(self):
        return int((parse_isoformat(self.arrival_time) - parse_isoformat(self.departure_time)).seconds / 60)
    
    def to_row(self):
        return Row(email_address=self.email_address,
                   departure_time=serialize_date(self.departure_time),
                   departure_airport_code=self.departure_airport_code,
                   arrival_time=serialize_date(self.arrival_time),
                   arrival_airport_code=self.arrival_airport_code,
                   flight_number=self.flight_number,
                   confirmation_code=self.confirmation_code,
                   airline=self.airline)
    
    @classmethod
    def from_row(cls, row: Row):
        return cls(
            email_address=row.email_address,
            departure_time=row.departure_time,
            departure_airport_code=row.departure_airport_code,
            arrival_time=row.arrival_time,
            arrival_airport_code=row.arrival_airport_code,
            flight_number=row.flight_number,
            confirmation_code=row.confirmation_code,
            airline=row.airline,
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
                "confirmation_code",
                "airline",
            ],
            field_types=[
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
            ],
        )

    @staticmethod
    def to_flyer_stats_data(row: Row):
        data = FlightData.from_row(row)
        return FlyerStatsData(
            data.email_address, 
            data.get_duration(), 
            1)
    
@dataclass
class FlyerStatsData:
    email_address: str
    total_flight_duration: int
    number_of_flights: int

    def __init__(self, email_address=None, total_flight_duration=0, number_of_flights=0, flight_data=None):
        if flight_data:
            self.email_address = flight_data.email_address
            self.total_flight_duration = self.get_duration()
            self.number_of_flights = 1
        else:
            self.email_address = email_address
            self.total_flight_duration = total_flight_duration
            self.number_of_flights = number_of_flights
    
    def merge(self, that):
        if self.email_address != that.email_address:
            raise ValueError("Cannot merge FlyerStatsData for different email addresses")

        merged = FlyerStatsData()
        merged.email_address = self.email_address
        merged.total_flight_duration = self.total_flight_duration + that.total_flight_duration
        merged.number_of_flights = self.number_of_flights + that.number_of_flights
        return merged
    
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
    