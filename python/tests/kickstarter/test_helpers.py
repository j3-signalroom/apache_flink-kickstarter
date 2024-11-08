import sys
import os
import random
import datetime
import pytest

# --- Add the source directory containing the 'model' package to the Python path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../../src/kickstarter')))

from model.flight_data import FlightData, FlyerStatsData
from model.airline_flight_data import AirlineFlightData


def generate_airport_code():
    airports = [
        "ATL", "DFW", "DEN", "ORD", "LAX", "CLT", "MCO", "LAS", "PHX", "MIA",
        "SEA", "IAH", "JFK", "EWR", "FLL", "MSP", "SFO", "DTW", "BOS", "SLC",
        "PHL", "BWI", "TPA", "SAN", "LGA", "MDW", "BNA", "IAD", "DCA", "AUS"
    ]
    return random.choice(airports)


def generate_string(size):
    alpha_string = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    return ''.join(random.choice(alpha_string) for _ in range(size))


def generate_email():
    return generate_string(10) + "@email.com"


def generate_departure_time():
    now = datetime.datetime.now()
    return now + datetime.timedelta(days=random.randint(0, 364), hours=random.randint(0, 23), minutes=random.randint(0, 59))


def generate_arrival_time(departure_time):
    return departure_time + datetime.timedelta(hours=random.randint(0, 14), minutes=random.randint(0, 59))


# Builder Classes
class AirlineFlightDataBuilder:
    def __init__(self):
        local_departure_time = generate_departure_time()
        local_arrival_time = generate_arrival_time(local_departure_time)

        self.email_address = generate_email()
        self.departure_time = local_departure_time.strftime("%Y-%m-%d %H:%M:%S")
        self.departure_airport_code = generate_airport_code()
        self.arrival_time = local_arrival_time.strftime("%Y-%m-%d %H:%M:%S")
        self.arrival_airport_code = generate_airport_code()
        self.flight_number = "SKY1" + str(random.randint(0, 999))
        self.confirmation_code = "SKY1" + generate_string(6)
        self.ticket_price = random.randint(100, 1000)
        self.aircraft = "Boeing 737"
        self.booking_agency_email = generate_email()

    def set_email_address(self, email_address):
        self.email_address = email_address
        return self

    def set_departure_time(self, departure_time):
        self.departure_time = departure_time
        return self

    def set_departure_airport_code(self, departure_airport_code):
        self.departure_airport_code = departure_airport_code
        return self

    def set_arrival_time(self, arrival_time):
        self.arrival_time = arrival_time
        return self

    def set_arrival_airport_code(self, arrival_airport_code):
        self.arrival_airport_code = arrival_airport_code
        return self

    def set_flight_number(self, flight_number):
        self.flight_number = flight_number
        return self

    def set_confirmation_code(self, confirmation_code):
        self.confirmation_code = confirmation_code
        return self
    
    def set_ticket_price(self, ticket_price):      
        self.ticket_price = ticket_price
        return self
    
    def set_aircraft(self, aircraft):
        self.aircraft = aircraft
        return self
    
    def set_booking_agency_email(self, booking_agency_email):
        self.booking_agency_email = booking_agency_email
        return self

    def build(self):
        return AirlineFlightData(
            email_address=self.email_address,
            departure_time=self.departure_time,
            departure_airport_code=self.departure_airport_code,
            arrival_time=self.arrival_time,
            arrival_airport_code=self.arrival_airport_code,
            flight_number=self.flight_number,
            confirmation_code=self.confirmation_code,
            ticket_price=self.ticket_price,
            aircraft=self.aircraft,
            booking_agency_email=self.booking_agency_email
        )


class FlightDataBuilder:
    def __init__(self):
        local_departure_time = generate_departure_time()
        local_arrival_time = generate_arrival_time(local_departure_time)

        self.email_address = generate_email()
        self.departure_time = local_departure_time.strftime("%Y-%m-%d %H:%M:%S")
        self.departure_airport_code = generate_airport_code()
        self.arrival_time = local_arrival_time.strftime("%Y-%m-%d %H:%M:%S")
        self.arrival_airport_code = generate_airport_code()
        self.flight_number = "Flight" + str(random.randint(0, 999))
        self.confirmation_code = "Confirmation" + generate_string(5)
        self.airline="skyOne"

    def set_email_address(self, email_address):
        self.email_address = email_address
        return self

    def set_departure_time(self, departure_time):
        self.departure_time = departure_time.strftime("%Y-%m-%d %H:%M:%S")
        return self

    def set_departure_airport_code(self, departure_airport_code):
        self.departure_airport_code = departure_airport_code
        return self

    def set_arrival_time(self, arrival_time):
        self.arrival_time = arrival_time.strftime("%Y-%m-%d %H:%M:%S")
        return self

    def set_arrival_airport_code(self, arrival_airport_code):
        self.arrival_airport_code = arrival_airport_code
        return self

    def set_flight_number(self, flight_number):
        self.flight_number = flight_number
        return self

    def set_confirmation_code(self, confirmation_code):
        self.confirmation_code = confirmation_code
        return self
    
    def set_airline(self, airline):
        self.airline = airline
        return self

    def build(self):
        return FlightData(
            email_address=self.email_address,
            departure_time=self.departure_time,
            departure_airport_code=self.departure_airport_code,
            arrival_time=self.arrival_time,
            arrival_airport_code=self.arrival_airport_code,
            flight_number=self.flight_number,
            confirmation_code=self.confirmation_code,
            airline=self.airline
        )


class FlyerStatsDataBuilder:
    def __init__(self):
        local_departure_time = generate_departure_time()
        local_arrival_time = generate_arrival_time(local_departure_time)

        self.email_address = generate_email()
        self.total_flight_duration = int((local_arrival_time - local_departure_time).total_seconds() / 60)
        self.number_of_flights = random.randint(0, 4)

    def set_email_address(self, email_address):
        self.email_address = email_address
        return self

    def set_total_flight_duration(self, total_flight_duration):
        self.total_flight_duration = total_flight_duration
        return self

    def set_number_of_flights(self, number_of_flights):
        self.number_of_flights = number_of_flights
        return self

    def build(self):
        return FlyerStatsData(
            email_address=self.email_address,
            total_flight_duration=self.total_flight_duration,
            number_of_flights=self.number_of_flights
        )


# Pytest test cases
@pytest.fixture
def airline_data_builder():
    return AirlineFlightDataBuilder()


@pytest.fixture
def flight_data_builder():
    return FlightDataBuilder()


@pytest.fixture
def flyer_stats_data_builder():
    return FlyerStatsDataBuilder()


def test_airline_data_builder(airline_data_builder):
    airline_data = airline_data_builder.build()
    assert airline_data.email_address.endswith("@email.com")
    assert len(airline_data.flight_number) > 0
    assert len(airline_data.confirmation_code) > 0


def test_flight_data_builder(flight_data_builder):
    flight_data = flight_data_builder.build()
    assert flight_data.email_address.endswith("@email.com")
    assert len(flight_data.flight_number) > 0
    assert len(flight_data.confirmation_code) > 0


def test_flyer_stats_data_builder(flyer_stats_data_builder):
    flyer_stats_data = flyer_stats_data_builder.build()
    assert flyer_stats_data.total_flight_duration > 0
    assert flyer_stats_data.number_of_flights >= 0
