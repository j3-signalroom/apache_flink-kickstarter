# Copyright (c) 2024 Jeffrey Jonathan Jennings
# Author: Jeffrey Jonathan Jennings (J3)

from dataclasses import dataclass, field
from datetime import datetime
import json


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
        return (
            self.email_address == other.email_address and
            self.departure_time == other.departure_time and
            self.departure_airport_code == other.departure_airport_code and
            self.arrival_time == other.arrival_time and
            self.arrival_airport_code == other.arrival_airport_code and
            self.flight_number == other.flight_number and
            self.confirmation_code == other.confirmation_code
        )

    def __hash__(self):
        return hash((
            self.email_address,
            self.departure_time,
            self.departure_airport_code,
            self.arrival_time,
            self.arrival_airport_code,
            self.flight_number,
            self.confirmation_code
        ))

    def __str__(self):
        return json.dumps(self.__dict__, default=str)
