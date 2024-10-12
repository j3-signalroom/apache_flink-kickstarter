/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import java.time.*;
import java.time.format.*;
import java.util.*;

import kickstarter.model.*;


public class TestHelpers {
    private static Random random = new Random(System.currentTimeMillis());

    public static String generateAirportCode() {
        String[] airports = new String[] {
                "ATL", "DFW", "DEN", "ORD", "LAX", "CLT", "MCO", "LAS", "PHX", "MIA",
                "SEA", "IAH", "JFK", "EWR", "FLL", "MSP", "SFO", "DTW", "BOS", "SLC",
                "PHL", "BWI", "TPA", "SAN", "LGA", "MDW", "BNA", "IAD", "DCA", "AUS"
        };

        return airports[random.nextInt(airports.length)];
    }

    public static String generateString(int size) {
        final String alphaString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        StringBuilder sb = new StringBuilder(size);

        for(int charCount = 0; charCount < size; charCount++) {
            final int index = random.nextInt(alphaString.length());
            sb.append(alphaString.charAt(index));
        }

        return sb.toString();
    }

    public static String generateEmail() {
        return generateString(10)+"@email.com";
    }

    public static LocalDateTime generateDepartureTime() {
        return LocalDateTime.now()
                .plusDays(random.nextInt(365))
                .withHour(random.nextInt(24))
                .withMinute(random.nextInt(60));
    }

    public static LocalDateTime generateArrivalTime(LocalDateTime departure) {
        return departure
                .plusHours(random.nextInt(15))
                .plusMinutes(random.nextInt(60));
    }

    public static class AirlineDataBuilder {
        private String emailAddress;
        private String departureTime;
        private String airportDepartureCode;
        private String arrivalTime;
        private String airportArrivalCode;
        private String flightNumber;
        private String confirmation;

        public AirlineDataBuilder() {
            LocalDateTime localDepartureTime = generateDepartureTime();
            LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);

            this.emailAddress = generateEmail();
            this.departureTime = localDepartureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.airportDepartureCode = generateAirportCode();
            this.arrivalTime = localArrivalTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.airportArrivalCode = generateAirportCode();
            this.flightNumber = "SKY1" + random.nextInt(1000);
            this.confirmation = "SKY1" + generateString(6);
        }

        public AirlineDataBuilder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public AirlineDataBuilder setDepartureTime(String departureTime) {
            this.departureTime = departureTime;
            return this;
        }

        public AirlineDataBuilder setDepartureAirportCode(String airportDepartureCode) {
            this.airportDepartureCode = airportDepartureCode;
            return this;
        }

        public AirlineDataBuilder setArrivalTime(String arrivalTime) {
            this.arrivalTime = arrivalTime;
            return this;
        }

        public AirlineDataBuilder setArrivalAirportCode(String airportArrivalCode) {
            this.airportArrivalCode = airportArrivalCode;
            return this;
        }

        public AirlineDataBuilder setFlightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public AirlineDataBuilder setConfirmation(String confirmation) {
            this.confirmation = confirmation;
            return this;
        }

        public AirlineData build() {
            AirlineData airlineData = new AirlineData();

            airlineData.setEmailAddress(this.emailAddress);
            airlineData.setDepartureTime(this.departureTime);
            airlineData.setDepartureAirportCode(this.airportDepartureCode);
            airlineData.setArrivalTime(this.arrivalTime);
            airlineData.setArrivalAirportCode(this.airportArrivalCode);
            airlineData.setFlightNumber(this.flightNumber);
            airlineData.setConfirmationCode(this.confirmation);

            return airlineData;
        }
    }

    public static class FlightDataBuilder {
        private String emailAddress;
        private String departureTime;
        private String departureAirportCode;
        private String arrivalTime;
        private String arrivalAirportCode;
        private String flightNumber;
        private String confirmationCode;

        public FlightDataBuilder() {
            LocalDateTime localDepartureTime = generateDepartureTime();
            LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);

            this.emailAddress = generateEmail();
            this.departureTime = localDepartureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.departureAirportCode = generateAirportCode();
            this.arrivalTime = localArrivalTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.arrivalAirportCode = generateAirportCode();
            this.flightNumber = "Flight" + random.nextInt(1000);
            this.confirmationCode = "Confirmation" + generateString(5);
        }

        public FlightDataBuilder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public FlightDataBuilder setDepartureTime(LocalDateTime departureTime) {
            this.departureTime = departureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return this;
        }

        public FlightDataBuilder setDepartureAirportCode(String departureAirportCode) {
            this.departureAirportCode = departureAirportCode;
            return this;
        }

        public FlightDataBuilder setArrivalTime(LocalDateTime arrivalTime) {
            this.arrivalTime = arrivalTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return this;
        }

        public FlightDataBuilder setArrivalAirportCode(String arrivalAirportCode) {
            this.arrivalAirportCode = arrivalAirportCode;
            return this;
        }

        public FlightDataBuilder setFlightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public FlightDataBuilder setConfirmationCode(String confirmationCode) {
            this.confirmationCode = confirmationCode;
            return this;
        }

        public FlightData build() {
            FlightData flightData = new FlightData();

            flightData.setEmailAddress(this.emailAddress);
            flightData.setDepartureTime(this.departureTime);
            flightData.setDepartureAirportCode(this.departureAirportCode);
            flightData.setArrivalTime(this.arrivalTime);
            flightData.setArrivalAirportCode(this.arrivalAirportCode);
            flightData.setFlightNumber(this.flightNumber);
            flightData.setConfirmationCode(this.confirmationCode);

            return flightData;
        }
    }

    public static class FlyerStatsDataBuilder {
        private String emailAddress;
        private long totalFlightDuration;
        private long numberOfFlights;

        public FlyerStatsDataBuilder() {
            final LocalDateTime localDepartureTime = generateDepartureTime();
            final LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);

            this.emailAddress = generateEmail();
            this.totalFlightDuration = Duration.between(localDepartureTime, localArrivalTime).toMinutes();
            this.numberOfFlights = random.nextInt(5);
        }

        public FlyerStatsDataBuilder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public FlyerStatsDataBuilder setTotalFlightDuration(long totalFlightDuration) {
            this.totalFlightDuration = totalFlightDuration;
            return this;
        }

        public FlyerStatsDataBuilder setNumberOfFlights(long numberOfFlights) {
            this.numberOfFlights = numberOfFlights;
            return this;
        }

        public FlyerStatsData build() {
            FlyerStatsData flyerStatsData = new FlyerStatsData();

            flyerStatsData.setEmailAddress(this.emailAddress);
            flyerStatsData.setTotalFlightDuration(this.totalFlightDuration);
            flyerStatsData.setNumberOfFlights(this.numberOfFlights);

            return flyerStatsData;
        }
    }
}