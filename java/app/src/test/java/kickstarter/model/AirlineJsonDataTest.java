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



class AirlineJsonDataTest {

    @Test
    void theClass_shouldBeSerializableAsAPOJO() {
        assertSerializedAsPojo(AirlineJsonData.class);
    }

    @Test
    void setters_shouldPopulateExpectedFields() {
        AirlineJsonData expected = new JsonTestHelpers.AirlineFlightDataBuilder().build();
        AirlineJsonData actual = new AirlineJsonData();
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
        AirlineJsonData flight1 = new JsonTestHelpers.AirlineFlightDataBuilder().build();
        AirlineJsonData flight2 = new AirlineJsonData();
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
        AirlineJsonData flight1 = new JsonTestHelpers.AirlineFlightDataBuilder().build();
        AirlineJsonData flight2 = new JsonTestHelpers.AirlineFlightDataBuilder().build();

        assertNotSame(flight1, flight2);
        assertNotEquals(flight1, flight2);
        assertNotEquals(flight1.hashCode(), flight2.hashCode());
    }

    @Test
    void toString_shouldReturnTheExpectedResults() {
        AirlineJsonData flightJsonData = new JsonTestHelpers.AirlineFlightDataBuilder().build();

        String expected = "AirlineJsonData{" +
                "email_address='" + flightJsonData.getEmailAddress() + '\'' +
                ", departure_time=" + flightJsonData.getDepartureTime() +
                ", departure_airport_code='" + flightJsonData.getDepartureAirportCode() + '\'' +
                ", arrival_time=" + flightJsonData.getArrivalTime() +
                ", arrival_airport_code='" + flightJsonData.getArrivalAirportCode() + '\'' +
                ", flight_number='" + flightJsonData.getFlightNumber() + '\'' +
                ", confirmation_code='" + flightJsonData.getConfirmationCode() + '\'' +
                '}';
        assertNotEquals(expected, flightJsonData.toString());
    }

    @Test
    void toFlightData_shouldConvertToAFlightDataObject() {
        AirlineJsonData skyOne = new JsonTestHelpers.AirlineFlightDataBuilder().build();
        FlightJsonData expected = new FlightJsonData();
        expected.setEmailAddress(skyOne.getEmailAddress());
        expected.setDepartureTime(skyOne.getDepartureTime());
        expected.setDepartureAirportCode(skyOne.getDepartureAirportCode());
        expected.setArrivalTime(skyOne.getArrivalTime());
        expected.setArrivalAirportCode(skyOne.getArrivalAirportCode());
        expected.setFlightNumber(skyOne.getFlightNumber());
        expected.setConfirmationCode(skyOne.getConfirmationCode());
        expected.setAirline("SkyOne");


        FlightJsonData actual = skyOne.toFlightData("SkyOne");

        assertEquals(expected, actual);
    }
}