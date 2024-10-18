/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import org.junit.jupiter.api.Test;
import static org.apache.flink.types.PojoTestUtils.assertSerializedAsPojo;
import static org.junit.jupiter.api.Assertions.*;

import kickstarter.*;



class AirlineFlightDataTest {

    @Test
    void theClass_shouldBeSerializableAsAPOJO() {
        assertSerializedAsPojo(AirlineData.class);
    }

    @Test
    void setters_shouldPopulateExpectedFields() {
        AirlineData expected = new TestHelpers.AirlineDataBuilder().build();
        AirlineData actual = new AirlineData();
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
        AirlineData flight1 = new TestHelpers.AirlineDataBuilder().build();
        AirlineData flight2 = new AirlineData();
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
        AirlineData flight1 = new TestHelpers.AirlineDataBuilder().build();
        AirlineData flight2 = new TestHelpers.AirlineDataBuilder().build();

        assertNotSame(flight1, flight2);
        assertNotEquals(flight1, flight2);
        assertNotEquals(flight1.hashCode(), flight2.hashCode());
    }

    @Test
    void toString_shouldReturnTheExpectedResults() {
        AirlineData flightData = new TestHelpers.AirlineDataBuilder().build();

        String expected = "AirlineData{" +
                "email_address='" + flightData.getEmailAddress() + '\'' +
                ", departure_time=" + flightData.getDepartureTime() +
                ", departure_airport_code='" + flightData.getDepartureAirportCode() + '\'' +
                ", arrival_time=" + flightData.getArrivalTime() +
                ", arrival_airport_code='" + flightData.getArrivalAirportCode() + '\'' +
                ", flight_number='" + flightData.getFlightNumber() + '\'' +
                ", confirmation_code='" + flightData.getConfirmationCode() + '\'' +
                '}';
        assertNotEquals(expected, flightData.toString());
    }

    @Test
    void toFlightData_shouldConvertToAFlightDataObject() {
        AirlineData skyOne = new TestHelpers.AirlineDataBuilder().build();
        FlightData expected = new FlightData();
        expected.setEmailAddress(skyOne.getEmailAddress());
        expected.setDepartureTime(skyOne.getDepartureTime());
        expected.setDepartureAirportCode(skyOne.getDepartureAirportCode());
        expected.setArrivalTime(skyOne.getArrivalTime());
        expected.setArrivalAirportCode(skyOne.getArrivalAirportCode());
        expected.setFlightNumber(skyOne.getFlightNumber());
        expected.setConfirmationCode(skyOne.getConfirmationCode());
        expected.setAirline("SkyOne");


        FlightData actual = skyOne.toFlightData("SkyOne");

        assertEquals(expected, actual);
    }
}