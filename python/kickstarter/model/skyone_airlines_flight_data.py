from dataclasses import dataclass
from datetime import datetime
import json
from utils import serialize
from pyflink.common import Row

from model.flight_data import FlightData

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


@dataclass
class SkyOneAirlinesFlightData:
    email_address: str | None
    flight_departure_time: datetime | None
    iata_departure_code: str | None
    flight_arrival_time: datetime | None
    iata_arrival_code: str | None
    flight_number: str | None
    confirmation: str | None
    ticket_price: float | None
    aircraft: str | None
    booking_agency_email: str | None

    def to_flight_data(self):
        return FlightData(email_address=self.email_address,
                          departure_time=self.flight_departure_time,
                          departure_airport_code=self.iata_departure_code,
                          arrival_time=self.flight_arrival_time,
                          arrival_airport_code=self.iata_arrival_code,
                          flight_number=self.flight_number,
                          confirmation_code=self.confirmation)
    
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
