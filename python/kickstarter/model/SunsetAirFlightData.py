# Copyright (c) 2024 Jeffrey Jonathan Jennings
# Author: Jeffrey Jonathan Jennings (J3)

from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Optional
from decimal import Decimal
import json


@dataclass
class SunsetAirFlightData:
    customer_email_address: Optional[str] = None
    departure_time: Optional[datetime] = None
    departure_airport: Optional[str] = None
    arrival_time: Optional[datetime] = None
    arrival_airport: Optional[str] = None
    flight_duration: Optional[timedelta] = None
    flight_id: Optional[str] = None
    reference_number: Optional[str] = None
    total_price: Optional[Decimal] = None
    aircraft_details: Optional[str] = None

    def to_flight_data(self):
        flight_data = FlightData(
            email_address=self.customer_email_address,
            departure_time=self.departure_time,
            departure_airport_code=self.departure_airport,
            arrival_time=self.arrival_time,
            arrival_airport_code=self.arrival_airport,
            flight_number=self.flight_id,
            confirmation_code=self.reference_number
        )
        return flight_data

    def __eq__(self, other):
        if not isinstance(other, SunsetAirFlightData):
            return False
        return (
            self.customer_email_address == other.customer_email_address and
            self.departure_time == other.departure_time and
            self.departure_airport == other.departure_airport and
            self.arrival_time == other.arrival_time and
            self.arrival_airport == other.arrival_airport and
            self.flight_duration == other.flight_duration and
            self.flight_id == other.flight_id and
            self.reference_number == other.reference_number and
            self.total_price == other.total_price and
            self.aircraft_details == other.aircraft_details
        )

    def __hash__(self):
        return hash((
            self.customer_email_address,
            self.departure_time,
            self.departure_airport,
            self.arrival_time,
            self.arrival_airport,
            self.flight_duration,
            self.flight_id,
            self.reference_number,
            self.total_price,
            self.aircraft_details
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
