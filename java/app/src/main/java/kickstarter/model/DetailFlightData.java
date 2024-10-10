package kickstarter.model;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.*;
import java.math.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailFlightData {
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

    // @JsonProperty("flight_duration")
    // private long flight_duration;

    @JsonProperty("flight_number")
    private String flight_number;

    @JsonProperty("confirmation_code")
    private String confirmation_code;

    // @JsonProperty("ticket_price")
    // private BigDecimal ticket_price;

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

    // public long getFlightDuration() {
    //     return this.flight_duration;
    // }

    // public void setFlightDuration(long flightDuration) {
    //     this.flight_duration = flightDuration;
    // }

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

    // public BigDecimal getTicketPrice() {
    //     return this.ticket_price;
    // }

    // public void setTicketPrice(BigDecimal totalPrice) {
    //     this.ticket_price = totalPrice;
    // }

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
}
