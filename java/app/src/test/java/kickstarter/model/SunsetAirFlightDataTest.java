/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import static org.apache.flink.types.PojoTestUtils.assertSerializedAsPojo;
import static org.junit.jupiter.api.Assertions.*;

import kickstarter.*;


class SunsetAirFlightDataTest {

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void theClass_shouldBeSerializableAsAPOJO() {
        assertSerializedAsPojo(SunsetAirFlightData.class);
    }

    @Test
    void setters_shouldPopulateExpectedFields() {
        SunsetAirFlightData expected = new TestHelpers.SunsetBuilder().build();
        SunsetAirFlightData actual = new SunsetAirFlightData();
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
        SunsetAirFlightData flight1 = new TestHelpers.SunsetBuilder().build();
        SunsetAirFlightData flight2 = new SunsetAirFlightData();
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
        SunsetAirFlightData flight1 = new TestHelpers.SunsetBuilder().build();
        SunsetAirFlightData flight2 = new TestHelpers.SunsetBuilder().build();

        assertNotSame(flight1, flight2);
        assertNotEquals(flight1, flight2);
        assertNotEquals(flight1.hashCode(), flight2.hashCode());
    }

    @Test
    void toFlightData_shouldConvertToAFlightDataObject() {
        SunsetAirFlightData sunset = new TestHelpers.SunsetBuilder().build();
        FlightData expected = new FlightData();
        expected.setEmailAddress(sunset.getEmailAddress());
        expected.setDepartureTime(sunset.getDepartureTime());
        expected.setDepartureAirportCode(sunset.getDepartureAirportCode());
        expected.setArrivalTime(sunset.getArrivalTime());
        expected.setArrivalAirportCode(sunset.getArrivalAirportCode());
        expected.setFlightNumber(sunset.getFlightNumber());
        expected.setConfirmationCode(sunset.getConfirmationCode());

        FlightData actual = sunset.toFlightData();

        assertEquals(expected, actual);
    }

    @Test
    void serializer_shouldSerializeAndDeserializeTheCorrectObject() throws Exception {
        SunsetAirFlightData original = new TestHelpers.SunsetBuilder().build();

        String serialized = mapper.writeValueAsString(original);
        SunsetAirFlightData deserialized = mapper.readValue(serialized, SunsetAirFlightData.class);

        assertSerializedAsPojo(SunsetAirFlightData.class);
        assertEquals(original, deserialized);
    }

    @Test
    void serializer_shouldHandleUnknownFields() throws Exception {
        String json = "{\"email_address\":\"LJNZGYPIER@email.com\",\"departure_time\":\"2023-10-16T22:25:00.000Z\",\"departure_airport_code\":\"LAS\",\"arrival_time\":\"2023-10-17T09:38:00.000Z\",\"arrival_airport_code\":\"BOS\",\"flight_number\":\"SKY1522\",\"confirmation_code\":\"SKY1OUJUUK\"}";

        SunsetAirFlightData object = mapper.readValue(json, SunsetAirFlightData.class);

        assertInstanceOf(SunsetAirFlightData.class, object);
    }
}
