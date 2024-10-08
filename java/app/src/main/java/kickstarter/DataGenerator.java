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

    private static ZonedDateTime generateDepartureTime() {
        return LocalDate.now()
                .plusDays(random.nextInt(365))
                .atTime(random.nextInt(24), random.nextInt(60))
                .atZone(ZoneId.of("UTC"));
    }

    private static ZonedDateTime generateArrivalTime(ZonedDateTime departure) {
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
        ZonedDateTime departureTime = generateDepartureTime();
        ZonedDateTime arrivalTime = generateArrivalTime(departureTime);

        airlineData.setEmailAddress(generateEmail());
        airlineData.setDepartureTime(departureTime.toString());
        airlineData.setDepartureAirportCode(generateAirportCode());
        airlineData.setArrivalTime(generateArrivalTime(departureTime).toString());
        airlineData.setArrivalAirportCode(generateAirportCode());
        airlineData.setFlightDuration(Duration.between(departureTime, arrivalTime).toMillis());
        airlineData.setFlightNumber(airlinePrefix + random.nextInt(1000));
        airlineData.setConfirmationCode(airlinePrefix + generateString(6));
        airlineData.setTicketPrice(BigDecimal.valueOf(500L + (long)random.nextInt(1000)));
        airlineData.setAircraft("Aircraft"+generateString(3));
        airlineData.setBookingAgencyEmail(generateEmail());

        return airlineData;
    }
}
