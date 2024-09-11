# Copyright (c) 2024 Jeffrey Jonathan Jennings
# Author: Jeffrey Jonathan Jennings (J3)

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
import json
from decimal import Decimal


@dataclass
class SkyOneAirlinesFlightData:
    email_address: Optional[str] = None
    flight_departure_time: Optional[datetime] = None
    iata_departure_code: Optional[str] = None
    flight_arrival_time: Optional[datetime] = None
    iata_arrival_code: Optional[str] = None
    flight_number: Optional[str] = None
    confirmation: Optional[str] = None
    ticket_price: Optional[float] = None
    aircraft: Optional[str] = None
    booking_agency_email: Optional[str] = None

    def __post_init__(self):
        pass

    def to_flight_data(self):
        flight_data = FlightData(
            email_address=self.email_address,
            departure_time=self.flight_departure_time,
            departure_airport_code=self.iata_departure_code,
            arrival_time=self.flight_arrival_time,
            arrival_airport_code=self.iata_arrival_code,
            flight_number=self.flight_number,
            confirmation_code=self.confirmation
        )
        return flight_data

    def __eq__(self, other):
        if not isinstance(other, SkyOneAirlinesFlightData):
            return False
        return (
            self.email_address == other.email_address and
            self.flight_departure_time == other.flight_departure_time and
            self.iata_departure_code == other.iata_departure_code and
            self.flight_arrival_time == other.flight_arrival_time and
            self.iata_arrival_code == other.iata_arrival_code and
            self.flight_number == other.flight_number and
            self.confirmation == other.confirmation and
            self.ticket_price == other.ticket_price and
            self.aircraft == other.aircraft and
            self.booking_agency_email == other.booking_agency_email
        )

    def __hash__(self):
        return hash((
            self.email_address,
            self.flight_departure_time,
            self.iata_departure_code,
            self.flight_arrival_time,
            self.iata_arrival_code,
            self.flight_number,
            self.confirmation,
            self.ticket_price,
            self.aircraft,
            self.booking_agency_email
        ))

    def __str__(self):
        return json.dumps(self.__dict__, default=str)


@dataclass
class FlightData:
    email_address: Optional[str] = None
    departure_time: Optional[datetime] = None
    departure_airport_code: Optional[str] = None
    arrival_time: Optional[datetime] = None
    arrival_airport_code: Optional[str] = None
    flight_number: Optional[str] = None
    confirmation_code: Optional[str] = None