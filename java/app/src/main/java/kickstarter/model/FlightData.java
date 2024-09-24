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
    private String emailAddress;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime departureTime;
    private String departureAirportCode;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime arrivalTime;
    private String arrivalAirportCode;
    private String flightNumber;
    private String confirmationCode;

    
    @JsonCreator
    public FlightData() {
        // --- Do nothing
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public ZonedDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(ZonedDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public String getDepartureAirportCodeCode() {
        return departureAirportCode;
    }

    public void setDepartureAirportCodeCode(String departureAirportCode) {
        this.departureAirportCode = departureAirportCode;
    }

    public ZonedDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(ZonedDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getArrivalAirportCodeCode() {
        return arrivalAirportCode;
    }

    public void setArrivalAirportCodeCode(String arrivalAirportCode) {
        this.arrivalAirportCode = arrivalAirportCode;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlightData that = (FlightData) o;
        return Objects.equals(emailAddress, that.emailAddress) && Objects.equals(departureTime, that.departureTime) && Objects.equals(departureAirportCode, that.departureAirportCode) && Objects.equals(arrivalTime, that.arrivalTime) && Objects.equals(arrivalAirportCode, that.arrivalAirportCode) && Objects.equals(flightNumber, that.flightNumber) && Objects.equals(confirmationCode, that.confirmationCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailAddress, departureTime, departureAirportCode, arrivalTime, arrivalAirportCode, flightNumber, confirmationCode);
    }

    @Override
    public String toString() {
        return "FlightData{" +
                "emailAddress='" + emailAddress + '\'' +
                ", departureTime=" + departureTime +
                ", departureAirportCode='" + departureAirportCode + '\'' +
                ", arrivalTime=" + arrivalTime +
                ", arrivalAirportCode='" + arrivalAirportCode + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", confirmationCode='" + confirmationCode + '\'' +
                '}';
    }
}