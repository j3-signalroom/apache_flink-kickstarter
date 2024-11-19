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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.apache.avro.generic.GenericFixed;

import kickstarter.helper.*;


@SuppressWarnings("all")
public class AirlineAvroData extends SpecificRecordBase implements SpecificRecord {
    public static final Schema SCHEMA = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"AirlineAvroData\",\"namespace\":\"kickstarter.model\",\"fields\":[{\"name\":\"email_address\",\"type\":\"string\"},{\"name\":\"departure_time\",\"type\":\"string\"},{\"name\":\"departure_airport_code\",\"type\":\"string\"},{\"name\":\"arrival_time\",\"type\":\"int\"},{\"name\":\"arrival_airport_code\",\"type\":\"string\"},{\"name\":\"flight_duration\",\"type\":\"long\"},{\"name\":\"flight_number\",\"type\":\"string\"},{\"name\":\"confirmation_code\",\"type\":\"string\"},{\"name\":\"ticket_price\",\"type\":{\"type\":\"bytes\",\"logicalType\":\"decimal\",\"precision\":10,\"scale\":2}},{\"name\":\"aircraft\",\"type\":\"string\"},{\"name\":\"booking_agency_email\",\"type\":\"string\"}]}");
    public static final String SUBJECT = "kickstarter.model.AirlineAvroData";

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
    /**
     * This method is used to compare two objects of the same type.
     * 
     * @param o The object to compare.
     * 
     * @return boolean True if the objects are equal, false otherwise.
     */
    public boolean equals(Object o) {
        if (this == o) 
            return true;
        if (o == null || getClass() != o.getClass()) 
            return false;
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
    /**
     * This method is used to generate a hash code for the object.
     * 
     * @return int The hash code for the object.
     */
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
    /**
     * This method is used by the serializer to get the string representation of the record.
     * 
     * @return The string representation of the record.
     */
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

    @Override
    /**
     * This method is used by the serializer to get the schema.
     * 
     * @return The schema of the record.
     */
    public Schema getSchema() {
        return SCHEMA;
    }

    @Override
    /**
     * This method is used by the serializer to get the values of the fields.
     * 
     * @param fieldIndex The index of the field to get.
     * 
     * @return The value of the field.
     */
    public Object get(int fieldIndex) {
        switch (fieldIndex) {
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
                throw new IndexOutOfBoundsException("Invalid field index: " + fieldIndex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    /**
     * This method is used by the deserializer to set the values of the fields.
     * 
     * @param fieldIndex The index of the field to set.
     * @param fieldValue The value to set the field to.
     */
    public void put(int fieldIndex, Object fieldValue) {
        switch (fieldIndex) {
            case 0: 
                setEmailAddress((String) fieldValue);
                break;
            case 1: 
                setDepartureTime(fieldValue.toString());
                break;
            case 2: 
                setDepartureAirportCode((String) fieldValue);
                break;
            case 3: 
                setArrivalTime((String) fieldValue);
                break;
            case 4: 
                setArrivalAirportCode(fieldValue.toString());
                break;
            case 5: 
                setFlightDuration((long) fieldValue);
                break;
            case 6: 
                setFlightNumber((String) fieldValue);
                break;
            case 7: 
                setConfirmationCode(fieldValue.toString());
                break;
            case 8:
                if (fieldValue instanceof ByteBuffer) {
                    ByteBuffer byteBuffer = (ByteBuffer) fieldValue;
                    byteBuffer.rewind();
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);
                    setTicketPrice(new BigDecimal(new String(bytes, StandardCharsets.UTF_8)));
                } else if (fieldValue instanceof BigDecimal) {
                    setTicketPrice((BigDecimal) fieldValue);
                } else {
                    throw new AvroRuntimeException("Unexpected type for ticket_price: " + fieldValue.getClass().getName());
                }
                break;
            case 9: 
                setAircraft(fieldValue.toString());
                break;
            case 10: 
                setBookingAgencyEmail((String) fieldValue); 
                break;
            default: 
                throw new IndexOutOfBoundsException("Invalid field index: " + fieldIndex);
        }
    }
}