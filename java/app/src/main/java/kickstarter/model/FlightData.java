/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.*;
import java.time.*;
import java.util.*;


public class FlightData {
    private String email_address;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime departure_time;
    private String departure_airport_code;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime arrival_time;
    private String arrival_airport_code;
    private String flight_number;
    private String confirmation_code;

    
    @JsonCreator
    public FlightData() {
        // --- Do nothing
    }

    public String getEmailAddress() {
        return this.email_address;
    }

    public void setEmailAddress(String EmailAddress) {
        this.email_address = EmailAddress;
    }

    public ZonedDateTime getDepartureTime() {
        return this.departure_time;
    }

    public void setDepartureTime(ZonedDateTime departureTime) {
        this.departure_time = departureTime;
    }

    public String getDepartureAirportCode() {
        return this.departure_airport_code;
    }

    public void setDepartureAirportCode(String departureAirport) {
        this.departure_airport_code = departureAirport;
    }

    public ZonedDateTime getArrivalTime() {
        return this.arrival_time;
    }

    public void setArrivalTime(ZonedDateTime arrivalTime) {
        this.arrival_time = arrivalTime;
    }

    public String getArrivalAirportCode() {
        return this.arrival_airport_code;
    }

    public void setArrivalAirportCode(String arrivalAirport) {
        this.arrival_airport_code = arrivalAirport;
    }

    public String getFlightNumber() {
        return this.flight_number;
    }

    public void setFlightNumber(String flightId) {
        this.flight_number = flightId;
    }

    public String getConfirmationCode() {
        return this.confirmation_code;
    }

    public void setConfirmationCode(String referenceNumber) {
        this.confirmation_code = referenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlightData that = (FlightData) o;
        return Objects.equals(this.email_address, that.email_address) && 
                Objects.equals(this.departure_time, that.departure_time) && 
                Objects.equals(this.departure_airport_code, that.departure_airport_code) && 
                Objects.equals(this.arrival_time, that.arrival_time) && 
                Objects.equals(this.arrival_airport_code, that.arrival_airport_code) && 
                Objects.equals(this.flight_number, that.flight_number) && 
                Objects.equals(this.confirmation_code, that.confirmation_code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.email_address,
                            this.departure_time, 
                            this.departure_airport_code, 
                            this.arrival_time, 
                            this.arrival_airport_code, 
                            this.flight_number, 
                            this.confirmation_code);
    }

    @Override
    public String toString() {
        return "FlightData{" +
                "email_address='" + this.email_address + '\'' +
                ", departure_time=" + this.departure_time +
                ", departure_airport_code='" + this.departure_airport_code + '\'' +
                ", arrival_time=" + this.arrival_time +
                ", arrival_airport_code='" + this.arrival_airport_code + '\'' +
                ", flight_number='" + this.flight_number + '\'' +
                ", confirmation_code='" + this.confirmation_code + '\'' +
                '}';
    }
}