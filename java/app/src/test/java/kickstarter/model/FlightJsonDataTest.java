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


class FlightJsonDataTest {

    @Test
    void theClass_shouldBeSerializableAsAPOJO() {
        assertSerializedAsPojo(FlightJsonData.class);
    }

    @Test
    void setters_shouldPopulateExpectedFields() {
        FlightJsonData expected = new JsonTestHelpers.FlightDataBuilder().build();
        FlightJsonData actual = new FlightJsonData();
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
        FlightJsonData flight1 = new JsonTestHelpers.FlightDataBuilder().build();
        FlightJsonData flight2 = new FlightJsonData();
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
        FlightJsonData flight1 = new JsonTestHelpers.FlightDataBuilder().build();
        FlightJsonData flight2 = new JsonTestHelpers.FlightDataBuilder().build();

        assertNotSame(flight1, flight2);
        assertNotEquals(flight1, flight2);
        assertNotEquals(flight1.hashCode(), flight2.hashCode());
    }

    @Test
    void toString_shouldReturnTheExpectedResults() {
        FlightJsonData flightJsonData = new JsonTestHelpers.FlightDataBuilder().build();

        String expected = "FlightJsonData{" +
                "emailAddress='" + flightJsonData.getEmailAddress() + "'" +
                ", departureTime=" + flightJsonData.getDepartureTime() +
                ", departureAirportCode='" + flightJsonData.getDepartureAirportCode() + "'}" +
                ", arrivalTime=" + flightJsonData.getArrivalTime() +
                ", arrivalAirportCode='" + flightJsonData.getArrivalAirportCode() + "'" +
                ", flightNumber='" + flightJsonData.getFlightNumber() + "'" +
                ", confirmationCode='" + flightJsonData.getConfirmationCode() + "'" +
                ", airline='" + flightJsonData.getAirline() + "'}";

        System.out.println(flightJsonData.toString());

        assertEquals(expected, flightJsonData.toString());
    }
}