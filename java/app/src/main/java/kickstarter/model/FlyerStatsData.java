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
import java.time.*;
import java.time.format.*;
import java.util.*;


public class FlyerStatsData implements Serializable {
    @JsonProperty("email_address")
    private String email_address;

    @JsonProperty("total_flight_duration")
    private long total_flight_duration;

    @JsonProperty("number_of_flights")
    private long number_of_flights;

    public FlyerStatsData() {
    }

    public FlyerStatsData(FlightData flightData) {
        this.email_address = flightData.getEmailAddress();
        this.total_flight_duration = Duration.between(LocalDateTime.parse(flightData.getDepartureTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                                                      LocalDateTime.parse(flightData.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).toMinutes();
        this.number_of_flights = 1;
    }

    public String getEmailAddress() {
        return this.email_address;
    }

    public void setEmailAddress(String emailAddress) {
        this.email_address = emailAddress;
    }

    public long getTotalFlightDuration() {
        return this.total_flight_duration;
    }

    public void setTotalFlightDuration(long totalFlightDuration) {
        this.total_flight_duration = totalFlightDuration;
    }

    public long getNumberOfFlights() {
        return this.number_of_flights;
    }

    public void setNumberOfFlights(long numberOfFlights) {
        this.number_of_flights = numberOfFlights;
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
        FlyerStatsData that = (FlyerStatsData) o;

        return this.getNumberOfFlights() == that.getNumberOfFlights() && 
                Objects.equals(this.getEmailAddress(), that.getEmailAddress()) &&
                Objects.equals(this.getTotalFlightDuration(), that.getTotalFlightDuration());
    }

    /**
     * This method is used to generate a hash code for the object.
     * 
     * @return int The hash code for the object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.getEmailAddress(), this.getTotalFlightDuration(), this.getNumberOfFlights());
    }

    /**
     * @return a string representation of the object.
     * Useful for debugging and logging.
     */
    @Override
    public String toString() {
        return "FlyerStatsData{" +
                "email_address='" + this.getEmailAddress() + "'" +
                ", totalFlightDuration=" + this.getTotalFlightDuration() +
                ", number_of_flights=" + this.getNumberOfFlights() +
                '}';
    }

    public FlyerStatsData merge(FlyerStatsData that) {
        if(this.getEmailAddress().equals(that.getEmailAddress())) {
            FlyerStatsData merged = new FlyerStatsData();

            merged.setEmailAddress(this.getEmailAddress());
            merged.setTotalFlightDuration(this.getTotalFlightDuration() + that.getTotalFlightDuration());
            merged.setNumberOfFlights(this.getNumberOfFlights() + that.getNumberOfFlights());

            return merged;
        } else {
            throw new IllegalArgumentException("Cannot merge FlyerStatsData for different email addresses");
        }
    }
}