from pyflink.common import Row, Types
from datetime import datetime
from dataclasses import dataclass
from decimal import Decimal

from helper.utilities import serialize_date
from model.flight_data import FlightData

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


@dataclass
class AirlineFlightData():
    email_address: str
    departure_time: str
    departure_airport_code: str
    arrival_time: str
    arrival_airport_code: str
    flight_number: str
    confirmation_code: str
    ticket_price: Decimal
    aircraft: str
    booking_agency_email: str

    
    @staticmethod
    def to_flight_data(airline_name: str, flight_data):
        return FlightData(
            airline=airline_name,
            email_address=flight_data.email_address,
            departure_time=flight_data.departure_time,
            departure_airport_code=flight_data.departure_airport_code,
            arrival_time=flight_data.arrival_time,
            arrival_airport_code=flight_data.arrival_airport_code,
            flight_number=flight_data.flight_number,
            confirmation_code=flight_data.confirmation_code
        )
    
    @classmethod
    def from_row(cls, row: Row):
        return cls(email_address=row.email_address,
                   departure_time=row.departure_time,
                   departure_airport_code=row.departure_airport_code,
                   arrival_time=row.arrival_time,
                   arrival_airport_code=row.arrival_airport_code,
                   flight_number=row.flight_number,
                   confirmation_code=row.confirmation_code,
                   ticket_price=row.ticket_price,
                   aircraft=row.aircraft,
                   booking_agency_email=row.booking_agency_email)
    
    def to_row(self):
        return Row(email_address=self.email_address,
                   departure_time=serialize_date(self.departure_time),
                   departure_airport_code=self.departure_airport_code,
                   arrival_time=serialize_date(self.arrival_time),
                   arrival_airport_code=self.arrival_airport_code,
                   flight_number=self.flight_number,
                   confirmation_code=self.confirmation_code,
                   ticket_price=self.ticket_price,
                   aircraft=self.aircraft,
                   booking_agency_email=self.booking_agency_email)
    
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
                "ticket_price",
                "aircraft",
                "booking_agency_email",
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
                Types.STRING(),
                Types.STRING(),
            ],
        )
