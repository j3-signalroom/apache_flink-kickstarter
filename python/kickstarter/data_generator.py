import random
import string
from datetime import datetime, timedelta
from decimal import Decimal
from typing import List
from model import SkyOneAirlinesFlightData, SunsetAirFlightData

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@signalroom.ai"
__status__     = "dev"


class DataGenerator:
    _random = random.Random()
    _users: List[str] = [
        f"{''.join(random.choices(string.ascii_uppercase, k=5))}@email.com" 
        for _ in range(100)
    ]

    @staticmethod
    def _generate_airport_code() -> str:
        airports = [
            "ATL", "DFW", "DEN", "ORD", "LAX", "CLT", "MCO", "LAS", "PHX", "MIA",
            "SEA", "IAH", "JFK", "EWR", "FLL", "MSP", "SFO", "DTW", "BOS", "SLC",
            "PHL", "BWI", "TPA", "SAN", "LGA", "MDW", "BNA", "IAD", "DCA", "AUS"
        ]
        return random.choice(airports)

    @staticmethod
    def _generate_string(size: int) -> str:
        return ''.join(random.choices(string.ascii_uppercase, k=size))

    @staticmethod
    def _generate_email() -> str:
        return random.choice(DataGenerator._users)

    @staticmethod
    def _generate_departure_time() -> datetime:
        now = datetime.utcnow()
        random_days = timedelta(days=random.randint(0, 365))
        random_hours = timedelta(hours=random.randint(0, 23))
        random_minutes = timedelta(minutes=random.randint(0, 59))
        return now + random_days + random_hours + random_minutes

    @staticmethod
    def _generate_arrival_time(departure: datetime) -> datetime:
        random_hours = timedelta(hours=random.randint(0, 14))
        random_minutes = timedelta(minutes=random.randint(0, 59))
        return departure + random_hours + random_minutes

    @staticmethod
    def generate_skyone_airlines_flight_data() -> SkyOneAirlinesFlightData:
        flight_data = SkyOneAirlinesFlightData()

        flight_data.email_address = DataGenerator._generate_email()
        flight_data.flight_departure_time = DataGenerator._generate_departure_time()
        flight_data.iata_departure_code = DataGenerator._generate_airport_code()
        flight_data.flight_arrival_time = DataGenerator._generate_arrival_time(flight_data.flight_departure_time)
        flight_data.iata_arrival_code = DataGenerator._generate_airport_code()
        flight_data.flight_number = f"SKY1{random.randint(0, 999)}"
        flight_data.confirmation = f"SKY1{DataGenerator._generate_string(6)}"
        flight_data.ticket_price = float(500 + random.randint(0, 999))
        flight_data.aircraft = f"Aircraft{DataGenerator._generate_string(3)}"
        flight_data.booking_agency_email = DataGenerator._generate_email()

        return flight_data

    @staticmethod
    def generate_sunset_air_flight_data() -> SunsetAirFlightData:
        flight_data = SunsetAirFlightData()

        flight_data.customer_email_address = DataGenerator._generate_email()
        flight_data.departure_time = DataGenerator._generate_departure_time()
        flight_data.departure_airport = DataGenerator._generate_airport_code()
        flight_data.arrival_time = DataGenerator._generate_arrival_time(flight_data.departure_time)
        flight_data.arrival_airport = DataGenerator._generate_airport_code()
        flight_data.flight_duration = flight_data.arrival_time - flight_data.departure_time
        flight_data.flight_id = f"SUN{random.randint(0, 999)}"
        flight_data.reference_number = f"SUN{DataGenerator._generate_string(8)}"
        flight_data.total_price = Decimal(300 + random.randint(0, 1499))
        flight_data.aircraft_details = f"Aircraft{DataGenerator._generate_string(4)}"

        return flight_data
