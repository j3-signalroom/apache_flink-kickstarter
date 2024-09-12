import random
import string
from datetime import datetime, timedelta
from decimal import Decimal
from typing import List
from model.skyone_airlines_flight_data import SkyOneAirlinesFlightData
from model.sunset_air_flight_data import SunsetAirFlightData

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@signalroom.ai"
__status__     = "dev"


class DataGenerator:
    def __init__(self) -> None:
        self._users = [f"{self._generate_string(size=5)}@email.com" for _ in range(100)]

    def _generate_airport_code(self) -> str:
        airports = ["ATL", "DFW", "DEN", "ORD", "LAX", "CLT", "MCO", "LAS", "PHX", "MIA",
                    "SEA", "IAH", "JFK", "EWR", "FLL", "MSP", "SFO", "DTW", "BOS", "SLC",
                    "PHL", "BWI", "TPA", "SAN", "LGA", "MDW", "BNA", "IAD", "DCA", "AUS"]
        return random.choice(airports)
    
    def _generate_string(self, size: int) -> str:
        return "".join(random.choice(string.ascii_uppercase) for i in range(size))
    
    def _generate_email(self) -> str:
        return random.choice(self._users)
    
    def _generate_departure_time(self) -> datetime:
        return datetime.now() + timedelta(days=random.randint(0, 365),
                                          hours=random.randint(0, 23),
                                          minutes=random.randint(0, 59))
    
    def _generate_arrival_time(self, departure: datetime) -> datetime:
        return departure + timedelta(hours=random.randint(0, 14),
                                     minutes=random.randint(0, 59))
    
    def generate_skyone_airlines_flight_data(self) -> SkyOneAirlinesFlightData:
        departure_time = self._generate_departure_time()
        return SkyOneAirlinesFlightData(email_address = self._generate_email(),
                                        flight_departure_time = departure_time,
                                        iata_departure_code = self._generate_airport_code(),
                                        flight_arrival_time = self._generate_arrival_time(departure_time),
                                        iata_arrival_code = self._generate_airport_code(),
                                        flight_number = f"SKY1{random.randint(0, 999)}",
                                        confirmation = f"SKY1{self._generate_string(6)}",
                                        ticket_price = float(500 + random.randint(0, 999)),
                                        aircraft = f"Aircraft{self._generate_string(3)}",
                                        booking_agency_email = self._generate_email())
    
    def generate_sunset_air_flight_data(self) -> SunsetAirFlightData:
        departure_time = self._generate_departure_time()
        arrival_time = self.generate_arrival_time(departure_time)
        return SunsetAirFlightData(customer_email_address = self._generate_email(),
                                   departure_time = departure_time,
                                   departure_airport = self._generate_airport_code(),
                                   arrival_time = self._generate_arrival_time(departure_time),
                                   arrival_airport = self._generate_airport_code(),
                                   flight_duration = arrival_time - departure_time,
                                   flight_id = f"SUN{random.randint(0, 999)}",
                                   reference_number = f"SUN{self._generate_string(8)}",
                                   total_price = Decimal(300 + random.randint(0, 1499)),
                                   aircraft_details = f"Aircraft{self._generate_string(4)}")
    
    