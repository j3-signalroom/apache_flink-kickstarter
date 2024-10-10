package kickstarter.model;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.*;
import java.math.*;
import java.util.*;


public class AirlineData {
    @JsonProperty("email_address")
    private String email_address;

    @JsonProperty("departure_time")
    private String departure_time;

    @JsonProperty("departure_airport_code")
    private String departure_airport_code;

    @JsonProperty("arrival_time")
    private String arrival_time;

    @JsonProperty("arrival_airport_code")
    private String arrival_airport_code;

    @JsonProperty("flight_duration")
    private long flight_duration;

    @JsonProperty("flight_number")
    private String flight_number;

    @JsonProperty("confirmation_code")
    private String confirmation_code;

    @JsonProperty("ticket_price")
    private BigDecimal ticket_price;

    @JsonProperty("aircraft")
    private String aircraft;

    @JsonProperty("booking_agency_email")
    private String booking_agency_email;


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
        AirlineData that = (AirlineData) o;
        return Objects.equals(this.email_address, that.email_address) && 
                                Objects.equals(this.departure_time, that.departure_time) && 
                                Objects.equals(this.departure_airport_code, that.departure_airport_code) && 
                                Objects.equals(this.arrival_time, that.arrival_time) && 
                                Objects.equals(this.arrival_airport_code, that.arrival_airport_code) && 
                                Objects.equals(this.flight_duration, that.flight_duration) && 
                                Objects.equals(this.flight_number, that.flight_number) && 
                                Objects.equals(this.confirmation_code, that.confirmation_code) && 
                                Objects.equals(this.ticket_price, that.ticket_price) && 
                                Objects.equals(this.aircraft, that.aircraft) &&
                                Objects.equals(this.booking_agency_email, that.booking_agency_email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.email_address,
                            this.departure_time, 
                            this.departure_airport_code, 
                            this.arrival_time, 
                            this.arrival_airport_code, 
                            this.flight_duration, 
                            this.flight_number, 
                            this.confirmation_code, 
                            this.ticket_price, 
                            this.aircraft,
                            this.booking_agency_email);
    }

    @Override
    public String toString() {
        return "AirlineData{" +
            "email_address='" + this.email_address + '\'' +
            ", departure_time=" + this.departure_time +
            ", departure_airport_code='" + this.departure_airport_code + '\'' +
            ", arrival_time=" + this.arrival_time +
            ", arrival_airport_code='" + this.arrival_airport_code + '\'' +
            ", flight_duration=" + this.flight_duration +
            ", flight_number='" + this.flight_number + '\'' +
            ", confirmation_code='" + this.confirmation_code + '\'' +
            ", ticket_price=" + this.ticket_price +
            ", aircraft='" + this.aircraft + '\'' +
            ", booking_agency_email='" + booking_agency_email + '\'' +
            '}';
    }
    public FlightData toFlightData() {
        FlightData flightData = new FlightData();

        flightData.setEmailAddress(getEmailAddress());
        flightData.setDepartureTime(getDepartureTime());
        flightData.setDepartureAirportCode(getDepartureAirportCode());
        flightData.setArrivalTime(getArrivalTime());
        flightData.setArrivalAirportCode(getArrivalAirportCode());
        flightData.setFlightNumber(getFlightNumber());
        flightData.setConfirmationCode(getConfirmationCode());

        return flightData;
    }
}
