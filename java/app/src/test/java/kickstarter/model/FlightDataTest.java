/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.flink.types.PojoTestUtils.*;

import kickstarter.*;


class FlightDataTest {

    @Test
    void theClass_shouldBeSerializableAsAPOJO() {
        assertSerializedAsPojo(FlightData.class);
    }

    @Test
    void setters_shouldPopulateExpectedFields() {
        FlightData expected = new TestHelpers.FlightDataBuilder().build();
        FlightData actual = new FlightData();
        actual.setEmailAddress(expected.getEmailAddress());
        actual.setDepartureTime(expected.getDepartureTime());
        actual.setDepartureAirportCode(expected.getDepartureAirportCode());
        actual.setArrivalTime(expected.getArrivalTime());
        actual.setArrivalAirportCode(expected.getArrivalAirportCode());
        actual.setFlightNumber(expected.getFlightNumber());
        actual.setConfirmationCode(expected.getConfirmationCode());
        actual.setAirline(expected.getAirline());

        assertEquals(expected.getEmailAddress(), actual.getEmailAddress());
        assertEquals(expected.getDepartureTime(), actual.getDepartureTime());
        assertEquals(expected.getDepartureAirportCode(), actual.getDepartureAirportCode());
        assertEquals(expected.getArrivalTime(), actual.getArrivalTime());
        assertEquals(expected.getArrivalAirportCode(), actual.getArrivalAirportCode());
        assertEquals(expected.getFlightNumber(), actual.getFlightNumber());
        assertEquals(expected.getConfirmationCode(), actual.getConfirmationCode());
        assertEquals(expected.getAirline(), actual.getAirline());
    }

    @Test
    void equals_shouldReturnTrue_forTwoEquivalentFlights() {
        FlightData flight1 = new TestHelpers.FlightDataBuilder().build();
        FlightData flight2 = new FlightData();
        flight2.setEmailAddress(flight1.getEmailAddress());
        flight2.setDepartureTime(flight1.getDepartureTime());
        flight2.setDepartureAirportCode(flight1.getDepartureAirportCode());
        flight2.setArrivalTime(flight1.getArrivalTime());
        flight2.setArrivalAirportCode(flight1.getArrivalAirportCode());
        flight2.setFlightNumber(flight1.getFlightNumber());
        flight2.setConfirmationCode(flight1.getConfirmationCode());
        flight2.setAirline(flight1.getAirline());

        assertNotSame(flight1, flight2);
        assertEquals(flight1, flight2);
        assertEquals(flight1.hashCode(), flight2.hashCode());
    }

    @Test
    void equals_shouldReturnFalse_forTwoDifferentFlights() {
        FlightData flight1 = new TestHelpers.FlightDataBuilder().build();
        FlightData flight2 = new TestHelpers.FlightDataBuilder().build();

        assertNotSame(flight1, flight2);
        assertNotEquals(flight1, flight2);
        assertNotEquals(flight1.hashCode(), flight2.hashCode());
    }

    @Test
    void toString_shouldReturnTheExpectedResults() {
        FlightData flightData = new TestHelpers.FlightDataBuilder().build();

        String expected = "FlightData{" +
                "email_address='" + flightData.getEmailAddress() + '\'' +
                ", departure_time=" + flightData.getDepartureTime() +
                ", departure_airport_code='" + flightData.getDepartureAirportCode() + '\'' +
                ", arrival_time=" + flightData.getArrivalTime() +
                ", arrival_airport_code='" + flightData.getArrivalAirportCode() + '\'' +
                ", flight_number='" + flightData.getFlightNumber() + '\'' +
                ", confirmation_code='" + flightData.getConfirmationCode() + '\'' +
                ", airline='" + flightData.getAirline() + '\'' +
                '}';

        System.out.println(flightData.toString());

        assertEquals(expected, flightData.toString());
    }
}
