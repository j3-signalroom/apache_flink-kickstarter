/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import com.fasterxml.jackson.annotation.*;
import org.apache.avro.specific.*;
import org.apache.avro.io.*;
import org.apache.avro.*;
import org.apache.avro.generic.*;
import io.confluent.kafka.schemaregistry.avro.*;
import java.math.*;
import java.util.*;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.avro.generic.GenericFixed;

import kickstarter.helper.*;


@SuppressWarnings("all")
public class AirlineAvroData extends SpecificRecordBase implements SpecificRecord {
    public static final Schema SCHEMA$ = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"AirlineData\",\"namespace\":\"com.thej3.apache_flink_kickstater.model\",\"fields\":[{\"name\":\"email_address\",\"type\":\"string\"},{\"name\":\"departure_time\",\"type\":\"string\"},{\"name\":\"departure_airport_code\",\"type\":\"string\"},{\"name\":\"arrival_time\",\"type\":\"int\"},{\"name\":\"arrival_airport_code\",\"type\":\"string\"},{\"name\":\"flight_duration\",\"type\":\"long\"},{\"name\":\"flight_number\",\"type\":\"string\"},{\"name\":\"confirmation_code\",\"type\":\"string\"},{\"name\":\"ticket_price\",\"type\":{\"type\":\"bytes\",\"logicalType\":\"decimal\",\"precision\":10,\"scale\":2}},{\"name\":\"aircraft\",\"type\":\"string\"},{\"name\":\"booking_agency_email\",\"type\":\"string\"}]}");

    // --- The field names of the class object
    public static final String FIELD_EMAIL_ADDRESS = "email_address";
    public static final String FIELD_DEPARTURE_TIME = "departure_time";
    public static final String FIELD_DEPARTURE_AIRPORT_CODE = "departure_airport_code";
	public static final String FIELD_ARRIVAL_TIME = "arrival_time";
    public static final String FIELD_ARRIVAL_AIRPORT_CODE = "arrival_airport_code";
    public static final String FIELD_FLIGHT_DURATION = "flight_duration";
    public static final String FIELD_FLIGHT_NUMBER = "flight_number";
    public static final String FIELD_CONFIRMATION_CODE = "confirmation_code";
    public static final String FIELD_TICKET_PRICE = "ticket_price";
    public static final String FIELD_AIRCRAFT = "aircraft";
    public static final String FIELD_BOOKING_AGENCY_EMAIL = "booking_agency_email";

    // --- The subject name of the class object
    public static final String NAMESPACE = "com.thej3.apache_flink_kickstater.model";
    public static final String SUBJECT = NAMESPACE + ".AirlineData";


     @JsonProperty(FIELD_EMAIL_ADDRESS)
    private String email_address;

    @JsonProperty(FIELD_DEPARTURE_TIME)
    private String departure_time;

    @JsonProperty(FIELD_DEPARTURE_AIRPORT_CODE)
    private String departure_airport_code;

    @JsonProperty(FIELD_ARRIVAL_TIME)
    private String arrival_time;

    @JsonProperty(FIELD_ARRIVAL_AIRPORT_CODE)
    private String arrival_airport_code;

    @JsonProperty(FIELD_FLIGHT_DURATION)
    private long flight_duration;

    @JsonProperty(FIELD_FLIGHT_NUMBER)
    private String flight_number;

    @JsonProperty(FIELD_CONFIRMATION_CODE)
    private String confirmation_code;

    @JsonProperty(FIELD_TICKET_PRICE)
    private BigDecimal ticket_price;

    @JsonProperty(FIELD_AIRCRAFT)
    private String aircraft;

    @JsonProperty(FIELD_BOOKING_AGENCY_EMAIL)
    private String booking_agency_email;


    /**
     * Default constructor.
     */
	public AirlineAvroData() {}


    public String getEmailAddress() {
        return this.email_address;
    }

    public void setEmailAddress(String emailAddress) {
        this.email_address = emailAddress;
    }

    public String getDepartureTime() {
        return this.departure_time;
    }

    public void setDepartureTime(String departureTime) {
        this.departure_time = departureTime;
    }

    public String getDepartureAirportCode() {
        return this.departure_airport_code;
    }

    public void setDepartureAirportCode(String departureAirport) {
        this.departure_airport_code = departureAirport;
    }

    public String getArrivalTime() {
        return this.arrival_time;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrival_time = arrivalTime;
    }

    public String getArrivalAirportCode() {
        return this.arrival_airport_code;
    }

    public void setArrivalAirportCode(String arrivalAirport) {
        this.arrival_airport_code = arrivalAirport;
    }

    public long getFlightDuration() {
        return this.flight_duration;
    }

    public void setFlightDuration(long flightDuration) {
        this.flight_duration = flightDuration;
    }

    public String getFlightNumber() {
        return this.flight_number;
    }

    public void setFlightNumber(String flightNumber) {
        this.flight_number = flightNumber;
    }

    public String getConfirmationCode() {
        return this.confirmation_code;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmation_code = confirmationCode;
    }

    public BigDecimal getTicketPrice() {
        return this.ticket_price;
    }

    public void setTicketPrice(BigDecimal totalPrice) {
        this.ticket_price = totalPrice;
    }

    public String getAircraft() {
        return this.aircraft;
    }

    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }

    public String getBookingAgencyEmail() {
        return this.booking_agency_email;
    }

    public void setBookingAgencyEmail(String bookingAgencyEmail) {
        this.booking_agency_email = bookingAgencyEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AirlineAvroData that = (AirlineAvroData) o;
        return Objects.equals(getEmailAddress(), that.email_address) && 
                                Objects.equals(getDepartureTime(), that.departure_time) && 
                                Objects.equals(getDepartureAirportCode(), that.departure_airport_code) && 
                                Objects.equals(getArrivalTime(), that.arrival_time) && 
                                Objects.equals(getArrivalAirportCode(), that.arrival_airport_code) && 
                                Objects.equals(getFlightDuration(), that.flight_duration) && 
                                Objects.equals(getFlightNumber(), that.flight_number) && 
                                Objects.equals(getConfirmationCode(), that.confirmation_code) && 
                                Objects.equals(getTicketPrice(), that.ticket_price) && 
                                Objects.equals(getAircraft(), that.aircraft) &&
                                Objects.equals(getBookingAgencyEmail(), that.booking_agency_email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEmailAddress(),
                            getDepartureTime(), 
                            getDepartureAirportCode(), 
                            getArrivalTime(), 
                            getArrivalAirportCode(),
                            getFlightDuration(), 
                            getFlightNumber(), 
                            getConfirmationCode(), 
                            getTicketPrice(), 
                            getAircraft(),
                            getBookingAgencyEmail());
    }

    @Override
    public String toString() {
        return "AirlineData{" +
            "email_address='" + getEmailAddress() + '\'' +
            ", departure_time=" + getDepartureTime() +
            ", departure_airport_code='" + getDepartureAirportCode() + '\'' +
            ", arrival_time=" + getArrivalTime() +
            ", arrival_airport_code='" + getArrivalAirportCode() + '\'' +
            ", flight_duration=" + getFlightDuration() +
            ", flight_number='" + getFlightNumber() + '\'' +
            ", confirmation_code='" + getConfirmationCode() + '\'' +
            ", ticket_price=" + getTicketPrice() +
            ", aircraft='" + getAircraft() + '\'' +
            ", booking_agency_email='" + getBookingAgencyEmail() + '\'' +
            '}';
    }
    public FlightData toFlightData(final String airline) {
        FlightData flightData = new FlightData();

        flightData.setEmailAddress(getEmailAddress());
        flightData.setDepartureTime(getDepartureTime());
        flightData.setDepartureAirportCode(getDepartureAirportCode());
        flightData.setArrivalTime(getArrivalTime());
        flightData.setArrivalAirportCode(getArrivalAirportCode());
        flightData.setFlightNumber(getFlightNumber());
        flightData.setConfirmationCode(getConfirmationCode());
        flightData.setAirline(airline);

        return flightData;
    }

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0: return email_address;
            case 1: return departure_time;
            case 2: return departure_airport_code;
            case 3: return arrival_time;
            case 4: return arrival_airport_code;
            case 5: return flight_duration;
            case 6: return flight_number;
            case 7: return confirmation_code;
            case 8: 
                return ByteBuffer.wrap(getTicketPrice().toString().getBytes(StandardCharsets.UTF_8));
            case 9: 
                return getAircraft();
            case 10: 
                return getBookingAgencyEmail();
            default: 
                throw new IndexOutOfBoundsException("Invalid field index: " + field$);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: email_address = (String) value$; break;
            case 1: departure_time = value$.toString(); break;
            case 2: departure_airport_code = (String) value$; break;
            case 3: arrival_time = (String) value$; break;
            case 4: arrival_airport_code = value$.toString(); break;
            case 5: flight_duration = (long) value$; break;
            case 6: flight_number = (String) value$; break;
            case 7: confirmation_code = value$.toString(); break;
            case 8:
                setTicketPrice(new BigDecimal(new String(ByteBuffer.wrap(value$.toString().getBytes(StandardCharsets.UTF_8)).array(), StandardCharsets.UTF_8)));
                break;
            case 9: 
                setAircraft(value$.toString());
                break;
            case 10: 
                setBookingAgencyEmail((String) value$); 
                break;
            default: 
                throw new IndexOutOfBoundsException("Invalid field index: " + field$);
        }
    }
}