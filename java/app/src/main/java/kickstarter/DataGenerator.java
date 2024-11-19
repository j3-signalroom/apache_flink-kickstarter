/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import java.math.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import kickstarter.model.*;


public class DataGenerator {
    private static Random random = new Random(System.currentTimeMillis());
    private static List<String> users = Stream
            .generate(() -> generateString(5)+"@email.com")
            .limit(100)
            .collect(Collectors.toList());

    private DataGenerator() {}
    
    private static String generateAirportCode() {
        String[] airports = new String[] {
                "ATL", "DFW", "DEN", "ORD", "LAX", "CLT", "MCO", "LAS", "PHX", "MIA",
                "SEA", "IAH", "JFK", "EWR", "FLL", "MSP", "SFO", "DTW", "BOS", "SLC",
                "PHL", "BWI", "TPA", "SAN", "LGA", "MDW", "BNA", "IAD", "DCA", "AUS"
        };

        return airports[random.nextInt(airports.length)];
    }

    private static String generateString(int size) {
        final String alphaString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        StringBuilder sb = new StringBuilder(size);

        for(int i = 0; i < size; i++) {
            final int index = random.nextInt(alphaString.length());
            sb.append(alphaString.charAt(index));
        }

        return sb.toString();
    }

    private static String generateEmail() {
        return users.get(random.nextInt(users.size()));
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

    /**
     * Generate an AirlineFlightData object.
     * 
     * @param airlinePrefix The prefix for the airline.
     * @return An AirlineFlightData object.
     */
    public static AirlineData generateAirlineFlightData(final String airlinePrefix) {
        AirlineData airlineData = new AirlineData();
        final LocalDateTime localDepartureTime = generateDepartureTime();
        final LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);

        airlineData.setEmailAddress(generateEmail());
        airlineData.setDepartureTime(localDepartureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        airlineData.setDepartureAirportCode(generateAirportCode());
        airlineData.setArrivalTime(localArrivalTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        airlineData.setArrivalAirportCode(generateAirportCode());
        airlineData.setFlightDuration(Duration.between(localDepartureTime, localArrivalTime).toMinutes());
        airlineData.setFlightNumber(airlinePrefix + random.nextInt(1000));
        airlineData.setConfirmationCode(airlinePrefix + generateString(6));
        airlineData.setTicketPrice(BigDecimal.valueOf(500L + (long)random.nextInt(1000)));
        airlineData.setAircraft("Aircraft" + generateString(3));
        airlineData.setBookingAgencyEmail(generateEmail());

        return airlineData;
    }

    /**
     * Generate an AirlineFlightData object.
     * 
     * @param airlinePrefix The prefix for the airline.
     * @return An AirlineFlightData object.
     */
    public static AirlineAvroData generateAirlineAvroData(final String airlinePrefix) {
        AirlineAvroData airlineData = new AirlineAvroData();
        final LocalDateTime localDepartureTime = generateDepartureTime();
        final LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);

        airlineData.setEmailAddress(generateEmail());
        airlineData.setDepartureTime(localDepartureTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        airlineData.setDepartureAirportCode(generateAirportCode());
        airlineData.setArrivalTime(localArrivalTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        airlineData.setArrivalAirportCode(generateAirportCode());
        airlineData.setFlightDuration(Duration.between(localDepartureTime, localArrivalTime).toMinutes());
        airlineData.setFlightNumber(airlinePrefix + random.nextInt(1000));
        airlineData.setConfirmationCode(airlinePrefix + generateString(6));
        airlineData.setTicketPrice(BigDecimal.valueOf(500L + (long)random.nextInt(1000)));
        airlineData.setAircraft("Aircraft" + generateString(3));
        airlineData.setBookingAgencyEmail(generateEmail());

        return airlineData;
    }
}
