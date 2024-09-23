from dataclasses import dataclass
from datetime import datetime
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
class SkyOneAirlinesFlightData:
    email_address: str | None
    flight_departure_time: str | None
    iata_departure_code: str | None
    flight_arrival_time: str | None
    iata_arrival_code: str | None
    flight_number: str | None
    confirmation: str | None
    ticket_price: int | None
    aircraft: str | None
    booking_agency_email: str | None

    def to_flight_data(self):
        return FlightData(email_address=self.email_address or "",
                          departure_time=self.flight_departure_time or "",
                          departure_airport_code=self.iata_departure_code or "",
                          arrival_time=self.flight_arrival_time or "",
                          arrival_airport_code=self.iata_arrival_code or "",
                          flight_number=self.flight_number or "",
                          confirmation_code=self.confirmation or "")
    
    def to_row(self):
        return Row(email_address=self.email_address or "",
                   flight_departure_time=self.flight_departure_time or "",
                   iata_departure_code=self.iata_departure_code or "",
                   flight_arrival_time=self.flight_arrival_time or "",
                   iata_arrival_code=self.iata_arrival_code or "",
                   flight_number=self.flight_number or "",
                   confirmation=self.confirmation or "",
                   ticket_price=self.ticket_price or 0,
                   aircraft=self.aircraft or "",
                   booking_agency_email=self.booking_agency_email or "")
    
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
    
    def __eq__(self, other):
        if not isinstance(other, SkyOneAirlinesFlightData):
            return False
        return (self.email_address == other.email_address and
                self.flight_departure_time == other.flight_departure_time and
                self.iata_departure_code == other.iata_departure_code and
                self.flight_arrival_time == other.flight_arrival_time and
                self.iata_arrival_code == other.iata_arrival_code and
                self.flight_number == other.flight_number and
                self.confirmation == other.confirmation and
                self.ticket_price == other.ticket_price and
                self.aircraft == other.aircraft and
                self.booking_agency_email == other.booking_agency_email)

    def __hash__(self):
        return hash((self.email_address,
                     self.flight_departure_time,
                     self.iata_departure_code,
                     self.flight_arrival_time,
                     self.iata_arrival_code,
                     self.flight_number,
                     self.confirmation,
                     self.ticket_price,
                     self.aircraft,
                     self.booking_agency_email))

    def __str__(self):
        return json.dumps(self.__dict__, default=str)
