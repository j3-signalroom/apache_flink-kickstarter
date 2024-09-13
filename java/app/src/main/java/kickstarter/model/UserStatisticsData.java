/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import java.time.*;
import java.util.*;


public class UserStatisticsData {
    private String emailAddress;
    private Duration totalFlightDuration;
    private long numberOfFlights;

    public UserStatisticsData() {
    }

    public UserStatisticsData(FlightData flightData) {
        this.emailAddress = flightData.getEmailAddress();
        this.totalFlightDuration = Duration.between(
                flightData.getDepartureTime(),
                flightData.getArrivalTime()
        );
        this.numberOfFlights = 1;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Duration getTotalFlightDuration() {
        return totalFlightDuration;
    }

    public void setTotalFlightDuration(Duration totalFlightDuration) {
        this.totalFlightDuration = totalFlightDuration;
    }

    public long getNumberOfFlights() {
        return numberOfFlights;
    }

    public void setNumberOfFlights(long numberOfFlights) {
        this.numberOfFlights = numberOfFlights;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStatisticsData that = (UserStatisticsData) o;
        return numberOfFlights == that.numberOfFlights && Objects.equals(emailAddress, that.emailAddress) && Objects.equals(totalFlightDuration, that.totalFlightDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailAddress, totalFlightDuration, numberOfFlights);
    }

    @Override
    public String toString() {
        return "UserStatistics{" +
                "emailAddress='" + emailAddress + '\'' +
                ", totalFlightDuration=" + totalFlightDuration +
                ", numberOfFlights=" + numberOfFlights +
                '}';
    }

    public UserStatisticsData merge(UserStatisticsData that) {
        if(this.emailAddress.equals(that.emailAddress)) {
            UserStatisticsData merged = new UserStatisticsData();

            merged.setEmailAddress(this.emailAddress);
            merged.setTotalFlightDuration(this.totalFlightDuration.plus(that.getTotalFlightDuration()));
            merged.setNumberOfFlights(this.numberOfFlights + that.getNumberOfFlights());

            return merged;
        } else {
            throw new IllegalArgumentException("Cannot merge UserStatisticsData for different email addresses");
        }
    }
}