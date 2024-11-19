/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

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

    // --- The subject name of the class object
    public static final String NAMESPACE = "com.thej3.apache_flink_kickstater.model";
    public static final String SUBJECT = NAMESPACE + ".AirlineData";


    private String email_address;
    private String departure_time;
    private String departure_airport_code;
    private String arrival_time;
    private String arrival_airport_code;
    private long flight_duration;
    private String flight_number;
    private String confirmation_code;
    private BigDecimal ticket_price;
    private String aircraft;
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
            case 0: 
                return getEmailAddress();
            case 1: 
                return getDepartureTime();
            case 2: 
                return getDepartureAirportCode();
            case 3: 
                return getArrivalTime();
            case 4: 
                return getArrivalAirportCode();
            case 5: 
                return getFlightDuration();
            case 6: 
                return getFlightNumber();
            case 7: 
                return getConfirmationCode();
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
            case 0: 
                setEmailAddress((String) value$);
                break;
            case 1: 
                setDepartureTime(value$.toString());
                break;
            case 2: 
                setDepartureAirportCode((String) value$);
                break;
            case 3: 
                setArrivalTime((String) value$);
                break;
            case 4: 
                setArrivalAirportCode(value$.toString());
                break;
            case 5: 
                setFlightDuration((long) value$);
                break;
            case 6: 
                setFlightNumber((String) value$);
                break;
            case 7: 
                setConfirmationCode(value$.toString());
                break;
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