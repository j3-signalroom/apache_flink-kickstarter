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


public class JsonTestHelpers {
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

    public static class AirlineFlightDataBuilder {
        private String emailAddress;
        private String departureTime;
        private String airportDepartureCode;
        private String arrivalTime;
        private String airportArrivalCode;
        private String flightNumber;
        private String confirmation;

        public AirlineFlightDataBuilder() {
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

        public AirlineFlightDataBuilder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public AirlineFlightDataBuilder setDepartureTime(String departureTime) {
            this.departureTime = departureTime;
            return this;
        }

        public AirlineFlightDataBuilder setDepartureAirportCode(String airportDepartureCode) {
            this.airportDepartureCode = airportDepartureCode;
            return this;
        }

        public AirlineFlightDataBuilder setArrivalTime(String arrivalTime) {
            this.arrivalTime = arrivalTime;
            return this;
        }

        public AirlineFlightDataBuilder setArrivalAirportCode(String airportArrivalCode) {
            this.airportArrivalCode = airportArrivalCode;
            return this;
        }

        public AirlineFlightDataBuilder setFlightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public AirlineFlightDataBuilder setConfirmation(String confirmation) {
            this.confirmation = confirmation;
            return this;
        }

        public AirlineJsonData build() {
            AirlineJsonData airlineJsonData = new AirlineJsonData();

            airlineJsonData.setEmailAddress(this.emailAddress);
            airlineJsonData.setDepartureTime(this.departureTime);
            airlineJsonData.setDepartureAirportCode(this.airportDepartureCode);
            airlineJsonData.setArrivalTime(this.arrivalTime);
            airlineJsonData.setArrivalAirportCode(this.airportArrivalCode);
            airlineJsonData.setFlightNumber(this.flightNumber);
            airlineJsonData.setConfirmationCode(this.confirmation);

            return airlineJsonData;
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

        public FlightJsonData build() {
            FlightJsonData flightJsonData = new FlightJsonData();

            flightJsonData.setEmailAddress(this.emailAddress);
            flightJsonData.setDepartureTime(this.departureTime);
            flightJsonData.setDepartureAirportCode(this.departureAirportCode);
            flightJsonData.setArrivalTime(this.arrivalTime);
            flightJsonData.setArrivalAirportCode(this.arrivalAirportCode);
            flightJsonData.setFlightNumber(this.flightNumber);
            flightJsonData.setConfirmationCode(this.confirmationCode);

            return flightJsonData;
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

        public FlyerStatsJsonData build() {
            FlyerStatsJsonData flyerStatsJsonData = new FlyerStatsJsonData();

            flyerStatsJsonData.setEmailAddress(this.emailAddress);
            flyerStatsJsonData.setTotalFlightDuration(this.totalFlightDuration);
            flyerStatsJsonData.setNumberOfFlights(this.numberOfFlights);

            return flyerStatsJsonData;
        }
    }
}