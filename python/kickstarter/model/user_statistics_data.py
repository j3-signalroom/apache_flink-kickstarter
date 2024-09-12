from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Optional

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@signalroom.ai"
__status__     = "dev"

@dataclass
class UserStatisticsData:
    email_address: str | None
    total_flight_duration: Optional[timedelta] = timedelta(0)
    number_of_flights: int = 0

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
                f"emailAddress='{self.email_address}', "
                f"totalFlightDuration={self.total_flight_duration}, "
                f"numberOfFlights={self.number_of_flights}}}")
