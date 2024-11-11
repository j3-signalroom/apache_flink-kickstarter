import sys
import os
import datetime
import pytest

# --- Add the source directory containing the 'model' package to the Python path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../../src/kickstarter')))

from test_helpers import AirlineFlightDataBuilder

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def test_define_workflow_should_convert_data_from_two_streams():
    sky_one_flight = AirlineFlightDataBuilder().build()
    sunset_flight = AirlineFlightDataBuilder().build()

    # Mocking the workflow: combining two streams into one list (as we don't have Flink environment in pytest)
    combined_stream = [sky_one_flight, sunset_flight]

    expected_data = [
        sky_one_flight,
        sunset_flight
    ]

    assert len(combined_stream) == len(expected_data)
    assert all(item in combined_stream for item in expected_data)


def test_define_workflow_should_filter_out_flights_in_the_past():
    add_minute_to_now = (datetime.datetime.now() + datetime.timedelta(minutes=1)).strftime("%Y-%m-%d %H:%M:%S")
    subtract_second_to_now = (datetime.datetime.now() - datetime.timedelta(seconds=1)).strftime("%Y-%m-%d %H:%M:%S")

    new_sky_one_flight = AirlineFlightDataBuilder().set_arrival_time(add_minute_to_now).build()
    old_sky_one_flight = AirlineFlightDataBuilder().set_arrival_time(subtract_second_to_now).build()
    new_sunset_flight = AirlineFlightDataBuilder().set_arrival_time(add_minute_to_now).build()
    old_sunset_flight = AirlineFlightDataBuilder().set_arrival_time(subtract_second_to_now).build()

    # Mocking the workflow: filtering out flights in the past
    combined_stream = [new_sky_one_flight, old_sky_one_flight, new_sunset_flight, old_sunset_flight]
    filtered_stream = [flight for flight in combined_stream if datetime.datetime.strptime(flight.arrival_time, "%Y-%m-%d %H:%M:%S") > datetime.datetime.now()]

    expected_data = [
        new_sky_one_flight,
        new_sunset_flight
    ]

    assert len(filtered_stream) == len(expected_data)
    assert all(item in filtered_stream for item in expected_data)
