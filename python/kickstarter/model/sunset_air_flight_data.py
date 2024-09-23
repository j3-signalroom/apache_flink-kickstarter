from dataclasses import dataclass
from datetime import datetime, timedelta
from decimal import Decimal
import json
from pyflink.common import Types, Row

from model.flight_data import FlightData
from common_functions import serialize

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


@dataclass
class SunsetAirFlightData:
    customer_email_address: str | None
    departure_time: datetime | None
    departure_airport: str | None
    arrival_time: datetime | None
    arrival_airport: str | None
    flight_duration: timedelta | None
    flight_id: str | None
    reference_number: str | None
    total_price: Decimal | None
    aircraft_details: str | None

    def to_flight_data(self):
        return FlightData(email_address=self.customer_email_address,
                          departure_time=self.departure_time,
                          departure_airport_code=self.departure_airport,
                          arrival_time=self.arrival_time,
                          arrival_airport_code=self.arrival_airport,
                          flight_number=self.flight_id,
                          confirmation_code=self.reference_number)
    
    def to_row(self):
        return Row(
            email_address=self.email_address,
            departure_time=serialize(self.flight_departure_time),
            iata_departure_code=self.iata_departure_code,
            arrival_time=serialize(self.flight_arrival_time),
            iata_arrival_code=self.iata_arrival_code,
            flight_number=self.flight_number,
            confirmation=self.confirmation,
            ticket_price=self.ticket_price,
            aircraft=self.aircraft,
            booking_agency_email=self.booking_agency_email
        )

    @staticmethod
    def get_value_type_info():
        return Types.ROW_NAMED(
            field_names=[
                "email_address",
                "flight_departure_time",
                "iata_departure_code",
                "flight_arrival_time",
                "iata_arrival_code",
                "flight_number",
                "confirmation",
                "ticket_price",
                "aircraft",
                "booking_agency_email",
            ],
            field_types=[
                Types.STRING(),
                Types.SQL_TIMESTAMP(),
                Types.STRING(),
                Types.SQL_TIMESTAMP(),
                Types.STRING(),
                Types.STRING(),
                Types.STRING(),
                Types.FLOAT(),
                Types.STRING(),
                Types.STRING(),
            ],
        )
    
    def __eq__(self, other):
        if not isinstance(other, SunsetAirFlightData):
            return False
        return (self.customer_email_address == other.customer_email_address and
                self.departure_time == other.departure_time and
                self.departure_airport == other.departure_airport and
                self.arrival_time == other.arrival_time and
                self.arrival_airport == other.arrival_airport and
                self.flight_duration == other.flight_duration and
                self.flight_id == other.flight_id and
                self.reference_number == other.reference_number and
                self.total_price == other.total_price and
                self.aircraft_details == other.aircraft_details)

    def __hash__(self):
        return hash(self.customer_email_address,
                    self.departure_time,
                    self.departure_airport,
                    self.arrival_time,
                    self.arrival_airport,
                    self.flight_duration,
                    self.flight_id,
                    self.reference_number,
                    self.total_price,
                    self.aircraft_details)

    def __str__(self):
        return json.dumps(self.__dict__, default=str)
