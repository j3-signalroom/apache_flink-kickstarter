/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;


public class FlyerStatsJsonData implements Serializable {
    private String emailAddress;
    private long totalFlightDuration;
    private long numberOfFlights;

    public FlyerStatsJsonData() {
    }

    public FlyerStatsJsonData(FlightJsonData flightJsonData) {
        this.emailAddress = flightJsonData.getEmailAddress();
        this.totalFlightDuration = Duration.between(LocalDateTime.parse(flightJsonData.getDepartureTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                                                    LocalDateTime.parse(flightJsonData.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).toMinutes();
        this.numberOfFlights = 1;
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public long getTotalFlightDuration() {
        return this.totalFlightDuration;
    }

    public void setTotalFlightDuration(long totalFlightDuration) {
        this.totalFlightDuration = totalFlightDuration;
    }

    public long getNumberOfFlights() {
        return this.numberOfFlights;
    }

    public void setNumberOfFlights(long numberOfFlights) {
        this.numberOfFlights = numberOfFlights;
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
        FlyerStatsJsonData that = (FlyerStatsJsonData) o;

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
        return "FlyerStatsJsonData{" +
                "emailAddress='" + this.getEmailAddress() + "'" +
                ", totalFlightDuration=" + this.getTotalFlightDuration() +
                ", numberOfFlights=" + this.getNumberOfFlights() +
                '}';
    }

    public FlyerStatsJsonData merge(FlyerStatsJsonData that) {
        if(this.getEmailAddress().equals(that.getEmailAddress())) {
            FlyerStatsJsonData merged = new FlyerStatsJsonData();

            merged.setEmailAddress(this.getEmailAddress());
            merged.setTotalFlightDuration(this.getTotalFlightDuration() + that.getTotalFlightDuration());
            merged.setNumberOfFlights(this.getNumberOfFlights() + that.getNumberOfFlights());

            return merged;
        } else {
            throw new IllegalArgumentException("Cannot merge FlyerStatsJsonData for different email addresses");
        }
    }
}