/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import java.io.*;
import java.util.*;


public class FlightJsonData implements Serializable {
    private String emailAddress;
    private String departureTime;
    private String departureAirportCode;
    private String arrivalTime;
    private String arrivalAirportCode;
    private String flightNumber;
    private String confirmationCode;
    private String airline;

    
    public FlightJsonData() {
        // --- Do nothing
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getDepartureTime() {
        return this.departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getDepartureAirportCode() {
        return this.departureAirportCode;
    }

    public void setDepartureAirportCode(String departureAirportCode) {
        this.departureAirportCode = departureAirportCode;
    }

    public String getArrivalTime() {
        return this.arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getArrivalAirportCode() {
        return this.arrivalAirportCode;
    }

    public void setArrivalAirportCode(String arrivalAirportCode) {
        this.arrivalAirportCode = arrivalAirportCode;
    }

    public String getFlightNumber() {
        return this.flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getConfirmationCode() {
        return this.confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
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
        FlightJsonData that = (FlightJsonData) o;
        
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
        return "FlightJsonData{" +
                "emailAddress='" + getEmailAddress() + "'" +
                ", departureTime=" + getDepartureTime() +
                ", departureAirportCode='" + getDepartureAirportCode() + "'}" +
                ", arrivalTime=" + getArrivalTime() +
                ", arrivalAirportCode='" + getArrivalAirportCode() + "'" +
                ", flightNumber='" + getFlightNumber() + "'" +
                ", confirmationCode='" + getConfirmationCode() + "'" +
                ", airline='" + getAirline() + "'}";
    }
}