/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.*;

import java.io.*;
import java.util.*;


public class FlightData implements Serializable {
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

    @JsonProperty("flight_number")
    private String flight_number;

    @JsonProperty("confirmation_code") 
    private String confirmation_code;

    @JsonProperty("airline") 
    private String airline;

    
    public FlightData() {
        // --- Do nothing
    }

    public String getEmailAddress() {
        return this.email_address;
    }

    public void setEmailAddress(String EmailAddress) {
        this.email_address = EmailAddress;
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

    public String getAirline() {
        return this.airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
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
                Objects.equals(this.confirmation_code, that.confirmation_code) &&
                Objects.equals(this.airline, that.airline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.email_address,
                            this.departure_time, 
                            this.departure_airport_code, 
                            this.arrival_time, 
                            this.arrival_airport_code, 
                            this.flight_number, 
                            this.confirmation_code,
                            this.airline);
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
                ", airline='" + this.airline + '\'' +
                '}';
    }
}