/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import kickstarter.model.AirlineJsonData;
import kickstarter.model.FlightJsonData;
import kickstarter.model.FlyerStatsJsonData;


/**
 * Test helper utilities for generating mock flight data.
 * Provides builder patterns for creating test instances of AirlineJsonData,
 * FlightJsonData, and FlyerStatsJsonData with randomized or custom values.
 */
public class JsonTestHelpers {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ALPHA_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String[] AIRPORT_CODES = new String[] {
        "ATL", "DFW", "DEN", "ORD", "LAX", "CLT", "MCO", "LAS", "PHX", "MIA",
        "SEA", "IAH", "JFK", "EWR", "FLL", "MSP", "SFO", "DTW", "BOS", "SLC",
        "PHL", "BWI", "TPA", "SAN", "LGA", "MDW", "BNA", "IAD", "DCA", "AUS"
    };

    /**
     * Generates a random airport code from a predefined list of major US airports.
     * 
     * @return A three-letter airport code (e.g., "ATL", "LAX")
     */
    public static String generateAirportCode() {
        return AIRPORT_CODES[ThreadLocalRandom.current().nextInt(AIRPORT_CODES.length)];
    }

    /**
     * Generates a random alphabetic string of the specified length.
     * 
     * @param size The length of the string to generate
     * @return A random uppercase alphabetic string
     */
    public static String generateString(int size) {
        StringBuilder sb = new StringBuilder(size);

        for (int charCount = 0; charCount < size; charCount++) {
            int index = ThreadLocalRandom.current().nextInt(ALPHA_STRING.length());
            sb.append(ALPHA_STRING.charAt(index));
        }

        return sb.toString();
    }

    /**
     * Generates a random email address for testing.
     * 
     * @return A random email address (e.g., "ABCDEFGHIJ@email.com")
     */
    public static String generateEmail() {
        return generateString(10) + "@email.com";
    }

    /**
     * Generates a random future departure time.
     * The departure time will be within the next 365 days with a random hour and minute.
     * 
     * @return A LocalDateTime representing a future departure time
     */
    public static LocalDateTime generateDepartureTime() {
        return LocalDateTime.now()
                .plusDays(ThreadLocalRandom.current().nextInt(365))
                .withHour(ThreadLocalRandom.current().nextInt(24))
                .withMinute(ThreadLocalRandom.current().nextInt(60));
    }

    /**
     * Generates a random arrival time based on a departure time.
     * The arrival time will be 0-21 hours after the departure time.
     * 
     * @param departure The departure time to calculate from
     * @return A LocalDateTime representing the arrival time
     */
    public static LocalDateTime generateArrivalTime(LocalDateTime departure) {
        return departure
                .plusHours(ThreadLocalRandom.current().nextInt(22))
                .plusMinutes(ThreadLocalRandom.current().nextInt(60));
    }

    /**
     * Builder class for creating test instances of AirlineJsonData.
     * Provides a fluent API for customizing flight data or using random defaults.
     */
    public static class AirlineFlightDataBuilder {
        private String emailAddress;
        private String departureTime;
        private String departureAirportCode;
        private String arrivalTime;
        private String arrivalAirportCode;
        private String flightNumber;
        private String confirmation;

        /**
         * Creates a new builder with randomized default values.
         */
        public AirlineFlightDataBuilder() {
            LocalDateTime localDepartureTime = generateDepartureTime();
            LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);

            this.emailAddress = generateEmail();
            this.departureTime = localDepartureTime.format(DATE_TIME_FORMATTER);
            this.departureAirportCode = generateAirportCode();
            this.arrivalTime = localArrivalTime.format(DATE_TIME_FORMATTER);
            this.arrivalAirportCode = generateAirportCode();
            this.flightNumber = "SKY1" + ThreadLocalRandom.current().nextInt(1000);
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

        public AirlineFlightDataBuilder setDepartureAirportCode(String departureAirportCode) {
            this.departureAirportCode = departureAirportCode;
            return this;
        }

        public AirlineFlightDataBuilder setArrivalTime(String arrivalTime) {
            this.arrivalTime = arrivalTime;
            return this;
        }

        public AirlineFlightDataBuilder setArrivalAirportCode(String arrivalAirportCode) {
            this.arrivalAirportCode = arrivalAirportCode;
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

        /**
         * Builds an AirlineJsonData instance with the configured values.
         * 
         * @return A new AirlineJsonData instance
         */
        public AirlineJsonData build() {
            AirlineJsonData airlineJsonData = new AirlineJsonData();

            airlineJsonData.setEmailAddress(this.emailAddress);
            airlineJsonData.setDepartureTime(this.departureTime);
            airlineJsonData.setDepartureAirportCode(this.departureAirportCode);
            airlineJsonData.setArrivalTime(this.arrivalTime);
            airlineJsonData.setArrivalAirportCode(this.arrivalAirportCode);
            airlineJsonData.setFlightNumber(this.flightNumber);
            airlineJsonData.setConfirmationCode(this.confirmation);

            return airlineJsonData;
        }
    }

    /**
     * Builder class for creating test instances of FlightJsonData.
     * Provides a fluent API for customizing flight data or using random defaults.
     */
    public static class FlightDataBuilder {
        private String emailAddress;
        private String departureTime;
        private String departureAirportCode;
        private String arrivalTime;
        private String arrivalAirportCode;
        private String flightNumber;
        private String confirmationCode;
        private String airline;

        /**
         * Creates a new builder with randomized default values.
         */
        public FlightDataBuilder() {
            LocalDateTime localDepartureTime = generateDepartureTime();
            LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);

            this.emailAddress = generateEmail();
            this.departureTime = localDepartureTime.format(DATE_TIME_FORMATTER);
            this.departureAirportCode = generateAirportCode();
            this.arrivalTime = localArrivalTime.format(DATE_TIME_FORMATTER);
            this.arrivalAirportCode = generateAirportCode();
            this.flightNumber = "Flight" + ThreadLocalRandom.current().nextInt(1000);
            this.confirmationCode = "Confirmation" + generateString(5);
            this.airline = "SkyOne";
        }

        public FlightDataBuilder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public FlightDataBuilder setDepartureTime(LocalDateTime departureTime) {
            this.departureTime = departureTime.format(DATE_TIME_FORMATTER);
            return this;
        }

        public FlightDataBuilder setDepartureAirportCode(String departureAirportCode) {
            this.departureAirportCode = departureAirportCode;
            return this;
        }

        public FlightDataBuilder setArrivalTime(LocalDateTime arrivalTime) {
            this.arrivalTime = arrivalTime.format(DATE_TIME_FORMATTER);
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

        public FlightDataBuilder setAirline(String airline) {
            this.airline = airline;
            return this;
        }

        /**
         * Builds a FlightJsonData instance with the configured values.
         * 
         * @return A new FlightJsonData instance
         */
        public FlightJsonData build() {
            FlightJsonData flightJsonData = new FlightJsonData();

            flightJsonData.setEmailAddress(this.emailAddress);
            flightJsonData.setDepartureTime(this.departureTime);
            flightJsonData.setDepartureAirportCode(this.departureAirportCode);
            flightJsonData.setArrivalTime(this.arrivalTime);
            flightJsonData.setArrivalAirportCode(this.arrivalAirportCode);
            flightJsonData.setFlightNumber(this.flightNumber);
            flightJsonData.setConfirmationCode(this.confirmationCode);
            flightJsonData.setAirline(this.airline);

            return flightJsonData;
        }
    }

    /**
     * Builder class for creating test instances of FlyerStatsJsonData.
     * Provides a fluent API for customizing flyer statistics or using random defaults.
     */
    public static class FlyerStatsDataBuilder {
        private String emailAddress;
        private long totalFlightDuration;
        private long numberOfFlights;

        /**
         * Creates a new builder with randomized default values.
         */
        public FlyerStatsDataBuilder() {
            LocalDateTime localDepartureTime = generateDepartureTime();
            LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);

            this.emailAddress = generateEmail();
            this.totalFlightDuration = Duration.between(localDepartureTime, localArrivalTime).toMinutes();
            this.numberOfFlights = ThreadLocalRandom.current().nextInt(5);
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

        /**
         * Builds a FlyerStatsJsonData instance with the configured values.
         * 
         * @return A new FlyerStatsJsonData instance
         */
        public FlyerStatsJsonData build() {
            FlyerStatsJsonData flyerStatsJsonData = new FlyerStatsJsonData();

            flyerStatsJsonData.setEmailAddress(this.emailAddress);
            flyerStatsJsonData.setTotalFlightDuration(this.totalFlightDuration);
            flyerStatsJsonData.setNumberOfFlights(this.numberOfFlights);

            return flyerStatsJsonData;
        }
    }
}