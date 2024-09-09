/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.kickstarter.model;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import static org.apache.flink.types.PojoTestUtils.assertSerializedAsPojo;
import static org.junit.jupiter.api.Assertions.*;

import apache_flink.kickstarter.*;


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
        actual.setCustomerEmailAddress(expected.getCustomerEmailAddress());
        actual.setDepartureTime(expected.getDepartureTime());
        actual.setDepartureAirport(expected.getDepartureAirport());
        actual.setArrivalTime(expected.getArrivalTime());
        actual.setArrivalAirport(expected.getArrivalAirport());
        actual.setFlightId(expected.getFlightId());
        actual.setReferenceNumber(expected.getReferenceNumber());

        assertEquals(expected.getCustomerEmailAddress(), actual.getCustomerEmailAddress());
        assertEquals(expected.getDepartureTime(), actual.getDepartureTime());
        assertEquals(expected.getDepartureAirport(), actual.getDepartureAirport());
        assertEquals(expected.getArrivalTime(), actual.getArrivalTime());
        assertEquals(expected.getArrivalAirport(), actual.getArrivalAirport());
        assertEquals(expected.getFlightId(), actual.getFlightId());
        assertEquals(expected.getReferenceNumber(), actual.getReferenceNumber());
    }
    @Test
    void equals_shouldReturnTrue_forTwoEquivalentFlights() {
        SunsetAirFlightData flight1 = new TestHelpers.SunsetBuilder().build();
        SunsetAirFlightData flight2 = new SunsetAirFlightData();
        flight2.setCustomerEmailAddress(flight1.getCustomerEmailAddress());
        flight2.setDepartureTime(flight1.getDepartureTime());
        flight2.setDepartureAirport(flight1.getDepartureAirport());
        flight2.setArrivalTime(flight1.getArrivalTime());
        flight2.setArrivalAirport(flight1.getArrivalAirport());
        flight2.setFlightId(flight1.getFlightId());
        flight2.setReferenceNumber(flight1.getReferenceNumber());

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
        expected.setEmailAddress(sunset.getCustomerEmailAddress());
        expected.setDepartureTime(sunset.getDepartureTime());
        expected.setDepartureAirportCode(sunset.getDepartureAirport());
        expected.setArrivalTime(sunset.getArrivalTime());
        expected.setArrivalAirportCode(sunset.getArrivalAirport());
        expected.setFlightNumber(sunset.getFlightId());
        expected.setConfirmationCode(sunset.getReferenceNumber());

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
        String json = "{\"customerEmailAddress\":\"LJNZGYPIER@email.com\",\"departureTime\":\"2023-10-16T22:25:00.000Z\",\"departureAirport\":\"LAS\",\"arrivalTime\":\"2023-10-17T09:38:00.000Z\",\"arrivalAirport\":\"BOS\",\"flightId\":\"SKY1522\",\"referenceNumber\":\"SKY1OUJUUK\"}";

        SunsetAirFlightData object = mapper.readValue(json, SunsetAirFlightData.class);

        assertInstanceOf(SunsetAirFlightData.class, object);
    }
}