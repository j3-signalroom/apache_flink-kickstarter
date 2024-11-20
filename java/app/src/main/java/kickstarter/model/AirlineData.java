/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import com.fasterxml.jackson.annotation.*;
import java.io.Serializable;
import java.math.*;
import java.util.*;


public class AirlineData implements Serializable {
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
	public AirlineData() {}


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

    
    /**
     * This method is used to compare two objects of the same type.
     * 
     * @param o The object to compare.
     * 
     * @return boolean True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) 
            return true;
        if (o == null || getClass() != o.getClass()) 
            return false;
        AirlineData that = (AirlineData) o;

        return Objects.equals(getEmailAddress(), that.getEmailAddress()) && 
                Objects.equals(getDepartureTime(), that.getDepartureTime()) && 
                Objects.equals(getDepartureAirportCode(), that.getDepartureAirportCode()) && 
                Objects.equals(getArrivalTime(), that.getArrivalTime()) && 
                Objects.equals(getArrivalAirportCode(), that.getArrivalAirportCode()) && 
                Objects.equals(getFlightDuration(), that.getFlightDuration()) && 
                Objects.equals(getFlightNumber(), that.getFlightNumber()) && 
                Objects.equals(getConfirmationCode(), that.getConfirmationCode()) && 
                Objects.equals(getTicketPrice(), that.getTicketPrice()) && 
                Objects.equals(getAircraft(), that.getAircraft()) &&
                Objects.equals(getBookingAgencyEmail(), that.getBookingAgencyEmail());
    }

    /**
     * This method is used to generate a hash code for the object.
     * 
     * @return int The hash code for the object.
     */
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

    /**
     * @return a string representation of the object.
     * Useful for debugging and logging.
     */
    @Override
    public String toString() {
        return "AirlineData{" +
            "email_address='" + getEmailAddress() + "'" +
            ", departure_time=" + getDepartureTime() +
            ", departure_airport_code='" + getDepartureAirportCode() + "'" +
            ", arrival_time=" + getArrivalTime() +
            ", arrival_airport_code='" + getArrivalAirportCode() + "'" +
            ", flight_duration=" + getFlightDuration() +
            ", flight_number='" + getFlightNumber() + "'" +
            ", confirmation_code='" + getConfirmationCode() + "'" +
            ", ticket_price=" + getTicketPrice() +
            ", aircraft='" + getAircraft() + "'" +
            ", booking_agency_email='" + getBookingAgencyEmail() + "'" +
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
}