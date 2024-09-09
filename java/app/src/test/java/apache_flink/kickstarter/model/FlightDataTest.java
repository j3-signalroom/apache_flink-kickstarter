/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.kickstarter.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.flink.types.PojoTestUtils.*;

import apache_flink.kickstarter.*;


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

        assertEquals(expected.getEmailAddress(), actual.getEmailAddress());
        assertEquals(expected.getDepartureTime(), actual.getDepartureTime());
        assertEquals(expected.getDepartureAirportCode(), actual.getDepartureAirportCode());
        assertEquals(expected.getArrivalTime(), actual.getArrivalTime());
        assertEquals(expected.getArrivalAirportCode(), actual.getArrivalAirportCode());
        assertEquals(expected.getFlightNumber(), actual.getFlightNumber());
        assertEquals(expected.getConfirmationCode(), actual.getConfirmationCode());
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
                "emailAddress='" + flightData.getEmailAddress() + '\'' +
                ", departureTime=" + flightData.getDepartureTime() +
                ", departureAirportCode='" + flightData.getDepartureAirportCode() + '\'' +
                ", arrivalTime=" + flightData.getArrivalTime() +
                ", arrivalAirportCode='" + flightData.getArrivalAirportCode() + '\'' +
                ", flightNumber='" + flightData.getFlightNumber() + '\'' +
                ", confirmationCode='" + flightData.getConfirmationCode() + '\'' +
                '}';

        System.out.println(flightData.toString());

        assertEquals(expected, flightData.toString());
    }
}