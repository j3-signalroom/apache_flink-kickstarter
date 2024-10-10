/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import java.time.*;
import java.util.Random;

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

        for(int i = 0; i < size; i++) {
            final int index = random.nextInt(alphaString.length());
            sb.append(alphaString.charAt(index));
        }

        return sb.toString();
    }

    public static String generateEmail() {
        return generateString(10)+"@email.com";
    }

    public static ZonedDateTime generateDepartureTime() {
        return LocalDate.now()
                .plusDays(random.nextInt(365))
                .atTime(random.nextInt(24), random.nextInt(60))
                .atZone(ZoneId.of("UTC"));
    }

    public static ZonedDateTime generateArrivalTime(ZonedDateTime departure) {
        return departure
                .plusHours(random.nextInt(15))
                .plusMinutes(random.nextInt(60));
    }

    public static Duration generateDuration() {
        return Duration.ofMinutes(random.nextInt(300));
    }

    public static class AirlineDataBuilder {
        private String emailAddress;
        private String flightDepartureTime;
        private String iataDepartureCode;
        private String flightArrivalTime;
        private String iataArrivalCode;
        private String flightNumber;
        private String confirmation;

        public AirlineDataBuilder() {
            ZonedDateTime departure = generateDepartureTime();

            this.emailAddress = generateEmail();
            this.flightDepartureTime = departure.toString();
            this.iataDepartureCode = generateAirportCode();
            this.flightArrivalTime = generateArrivalTime(departure).toString();
            this.iataArrivalCode = generateAirportCode();
            this.flightNumber = "SKY1"+random.nextInt(1000);
            this.confirmation = "SKY1"+generateString(6);
        }

        public AirlineDataBuilder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public AirlineDataBuilder setDepartureTime(String flightDepartureTime) {
            this.flightDepartureTime = flightDepartureTime;
            return this;
        }

        public AirlineDataBuilder setDepartureAirportCode(String iataDepartureCode) {
            this.iataDepartureCode = iataDepartureCode;
            return this;
        }

        public AirlineDataBuilder setArrivalTime(String flightArrivalTime) {
            this.flightArrivalTime = flightArrivalTime;
            return this;
        }

        public AirlineDataBuilder setArrivalAirportCode(String iataArrivalCode) {
            this.iataArrivalCode = iataArrivalCode;
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

            airlineData.setEmailAddress(emailAddress);
            airlineData.setDepartureTime(flightDepartureTime);
            airlineData.setDepartureAirportCode(iataDepartureCode);
            airlineData.setArrivalTime(flightArrivalTime);
            airlineData.setArrivalAirportCode(iataArrivalCode);
            airlineData.setFlightNumber(flightNumber);
            airlineData.setConfirmationCode(confirmation);

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
            ZonedDateTime departure = generateDepartureTime();

            emailAddress = generateEmail();
            departureTime = departure.toString();
            departureAirportCode = generateAirportCode();
            arrivalTime = generateArrivalTime(departure).toString();
            arrivalAirportCode = generateAirportCode();
            flightNumber = "Flight"+random.nextInt(1000);
            confirmationCode = "Confirmation"+generateString(5);
        }

        public FlightDataBuilder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public FlightDataBuilder setDepartureTime(String departureTime) {
            this.departureTime = departureTime;
            return this;
        }

        public FlightDataBuilder setDepartureAirportCode(String departureAirportCode) {
            this.departureAirportCode = departureAirportCode;
            return this;
        }

        public FlightDataBuilder setArrivalTime(String arrivalTime) {
            this.arrivalTime = arrivalTime;
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
        private Duration totalFlightDuration;
        private long numberOfFlights;

        public FlyerStatsDataBuilder() {
            this.emailAddress = generateEmail();
            ZonedDateTime departure = generateDepartureTime();
            ZonedDateTime arrival = generateArrivalTime(departure);
            this.totalFlightDuration = Duration.between(departure, arrival);
            this.numberOfFlights = random.nextInt(5);
        }

        public FlyerStatsDataBuilder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public FlyerStatsDataBuilder setTotalFlightDuration(Duration totalFlightDuration) {
            this.totalFlightDuration = totalFlightDuration;
            return this;
        }

        public FlyerStatsDataBuilder setNumberOfFlights(long numberOfFlights) {
            this.numberOfFlights = numberOfFlights;
            return this;
        }

        public FlyerStatsData build() {
            FlyerStatsData stats = new FlyerStatsData();

            stats.setEmailAddress(emailAddress);
            stats.setTotalFlightDuration(totalFlightDuration);
            stats.setNumberOfFlights(numberOfFlights);

            return stats;
        }
    }
}