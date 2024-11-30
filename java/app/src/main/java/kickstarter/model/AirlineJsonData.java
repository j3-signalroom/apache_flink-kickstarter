/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import java.io.Serializable;
import java.math.*;
import java.util.*;


public class AirlineJsonData implements Serializable {
    private String emailAddress;
    private String departureTime;
    private String departureAirportCode;
    private String arrivalTime;
    private String arrivalAirportCode;
    private long flightDuration;
    private String flightNumber;
    private String confirmationCode;
    private BigDecimal ticketPrice;
    private String aircraft;
    private String bookingAgencyEmail;


    /**
     * Default constructor.
     */
	public AirlineJsonData() {}


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

    public long getFlightDuration() {
        return this.flightDuration;
    }

    public void setFlightDuration(long flightDuration) {
        this.flightDuration = flightDuration;
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

    public BigDecimal getTicketPrice() {
        return this.ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getAircraft() {
        return this.aircraft;
    }

    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }

    public String getBookingAgencyEmail() {
        return this.bookingAgencyEmail;
    }

    public void setBookingAgencyEmail(String bookingAgencyEmail) {
        this.bookingAgencyEmail = bookingAgencyEmail;
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
        AirlineJsonData that = (AirlineJsonData) o;

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
        return "AirlineJsonData{" +
            "emailAddress='" + getEmailAddress() + "'" +
            ", departureTime=" + getDepartureTime() +
            ", departureAirportCode='" + getDepartureAirportCode() + "'" +
            ", arrivalTime=" + getArrivalTime() +
            ", arrivalAirportCode='" + getArrivalAirportCode() + "'" +
            ", flightDuration=" + getFlightDuration() +
            ", flightNumber='" + getFlightNumber() + "'" +
            ", confirmationCode='" + getConfirmationCode() + "'" +
            ", ticketPrice=" + getTicketPrice() +
            ", aircraft='" + getAircraft() + "'" +
            ", bookingAgencyEmail='" + getBookingAgencyEmail() + "'" +
            '}';
    }

    public FlightJsonData toFlightData(final String airline) {
        FlightJsonData flightData = new FlightJsonData();

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