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
        FlightData that = (FlightData) o;
        
        return Objects.equals(getEmailAddress(), that.getEmailAddress()) && 
                Objects.equals(getDepartureTime(), that.getDepartureTime()) && 
                Objects.equals(getDepartureAirportCode(), that.getDepartureAirportCode()) && 
                Objects.equals(getArrivalTime(), that.getArrivalTime()) && 
                Objects.equals(getArrivalAirportCode(), that.getArrivalAirportCode()) && 
                Objects.equals(getFlightNumber(), that.getFlightNumber()) && 
                Objects.equals(getConfirmationCode(), that.getConfirmationCode()) && 
                Objects.equals(getAirline(), that.getAirline());
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
                            getFlightNumber(), 
                            getConfirmationCode(),
                            getAirline());
    }

    /**
     * @return a string representation of the object.
     * Useful for debugging and logging.
     */
    @Override
    public String toString() {
        return "FlightData{" +
                "email_address='" + getEmailAddress() + "'" +
                ", departure_time=" + getDepartureTime() +
                ", departure_airport_code='" + getDepartureAirportCode() + "'" +
                ", arrival_time=" + getArrivalTime() +
                ", arrival_airport_code='" + getArrivalAirportCode() + "'" +
                ", flight_number='" + getFlightNumber() + "'" +
                ", confirmation_code='" + getConfirmationCode() + "'" +
                ", airline='" + getAirline() + "'" +
                '}';
    }
}