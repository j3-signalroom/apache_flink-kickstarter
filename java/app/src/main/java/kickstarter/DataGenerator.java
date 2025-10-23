/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import kickstarter.model.AirlineAvroData;
import kickstarter.model.AirlineJsonData;


/**
 * Thread-safe data generator for airline flight information.  ThreadLocalRandom
 * is thread-safe and provides better performance in multi-threaded environments. 
 * Each thread gets its own Random instance, preventing race conditions in Flink's 
 * parallel operations.
 */
public class DataGenerator {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ALPHA_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final List<String> USERS = Stream
            .generate(() -> generateString(5)+"@email.com")
            .limit(100)
            .collect(Collectors.toList());

    // --- Private constructor to prevent instantiation
    private DataGenerator() {}
    
    // Airport codes for realistic flight generation
    private static final String[] AIRPORTS = new String[] {
            "ATL", "DFW", "DEN", "ORD", "LAX", "CLT", "MCO", "LAS", "PHX", "MIA",
            "SEA", "IAH", "JFK", "EWR", "FLL", "MSP", "SFO", "DTW", "BOS", "SLC",
            "PHL", "BWI", "TPA", "SAN", "LGA", "MDW", "BNA", "IAD", "DCA", "AUS"
    };

    /**
     * Generates a random airport code.
     * 
     * @return A three-letter airport code
     */
    private static String generateAirportCode() {
        return AIRPORTS[ThreadLocalRandom.current().nextInt(AIRPORTS.length)];
    }

    /**
     * Generates a random string of the specified length using uppercase letters.
     * 
     * @param size The length of the string to generate
     * @return A random string of uppercase letters
     */
    private static String generateString(int size) {
        StringBuilder sb = new StringBuilder(size);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for(int i = 0; i < size; i++) {
            int index = random.nextInt(ALPHA_STRING.length());
            sb.append(ALPHA_STRING.charAt(index));
        }

        return sb.toString();
    }

    /**
     * Selects a random email address from the pre-generated list of users.
     * 
     * @return A random email address
     */
    private static String generateEmail() {
        return USERS.get(ThreadLocalRandom.current().nextInt(USERS.size()));
    }

    /**
     * Generates a random departure time within the next 365 days.
     * 
     * @return A LocalDateTime representing the departure time
     */
    public static LocalDateTime generateDepartureTime() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return LocalDateTime.now()
                .plusDays(random.nextInt(365))
                .withHour(random.nextInt(24))
                .withMinute(random.nextInt(60))
                .withSecond(0)
                .withNano(0);
    }

    /**
     * Generates a random arrival time based on the departure time.
     * Ensures the arrival is after departure with a flight duration between 1 and 15 hours.
     * 
     * @param departure The departure time
     * @return A LocalDateTime representing the arrival time
     */
    public static LocalDateTime generateArrivalTime(LocalDateTime departure) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        // --- Minimum 1 hour flight, maximum 21 hours
        return departure
                .plusHours(random.nextInt(1, 22))
                .plusMinutes(random.nextInt(60));
    }

    /**
     * Generate an AirlineJsonData object with realistic flight information.
     * 
     * @param airlinePrefix The prefix for the airline (e.g., "SKY1", "SUN")
     * @return An AirlineJsonData object with randomly generated flight data
     */
    public static AirlineJsonData generateAirlineJsonData(final String airlinePrefix) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        AirlineJsonData airlineData = new AirlineJsonData();
        
        final LocalDateTime localDepartureTime = generateDepartureTime();
        final LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);
        final String[] airportCodes = generateDifferentAirportCodes();

        airlineData.setEmailAddress(generateEmail());
        airlineData.setDepartureTime(localDepartureTime.format(DATE_TIME_FORMATTER));
        airlineData.setDepartureAirportCode(airportCodes[0]);
        airlineData.setArrivalTime(localArrivalTime.format(DATE_TIME_FORMATTER));
        airlineData.setArrivalAirportCode(airportCodes[1]);
        airlineData.setFlightDuration(Duration.between(localDepartureTime, localArrivalTime).toMinutes());
        airlineData.setFlightNumber(airlinePrefix + random.nextInt(1000));
        airlineData.setConfirmationCode(airlinePrefix + generateString(6));
        airlineData.setTicketPrice(BigDecimal.valueOf(500L + (long)random.nextInt(1000)));
        airlineData.setAircraft("Aircraft" + generateString(3));
        airlineData.setBookingAgencyEmail(generateEmail());

        return airlineData;
    }

    /**
     * Generate an AirlineAvroData object with realistic flight information.
     * 
     * @param airlinePrefix The prefix for the airline (e.g., "SKY1", "SUN")
     * @return An AirlineAvroData object with randomly generated flight data
     */
    public static AirlineAvroData generateAirlineAvroData(final String airlinePrefix) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        AirlineAvroData airlineData = new AirlineAvroData();
        
        final LocalDateTime localDepartureTime = generateDepartureTime();
        final LocalDateTime localArrivalTime = generateArrivalTime(localDepartureTime);
        final String[] airportCodes = generateDifferentAirportCodes();

        airlineData.setEmailAddress(generateEmail());
        airlineData.setDepartureTime(localDepartureTime.format(DATE_TIME_FORMATTER));
        airlineData.setDepartureAirportCode(airportCodes[0]);
        airlineData.setArrivalTime(localArrivalTime.format(DATE_TIME_FORMATTER));
        airlineData.setArrivalAirportCode(airportCodes[1]);
        airlineData.setFlightDuration(Duration.between(localDepartureTime, localArrivalTime).toMinutes());
        airlineData.setFlightNumber(airlinePrefix + random.nextInt(1000));
        airlineData.setConfirmationCode(airlinePrefix + generateString(6));
        
        // Convert BigDecimal to ByteBuffer for Avro
        BigDecimal ticketPrice = BigDecimal.valueOf(500L + (long)random.nextInt(1000));
        ByteBuffer ticketPriceBuffer = ByteBuffer.wrap(ticketPrice.unscaledValue().toByteArray());
        airlineData.setTicketPrice(ticketPriceBuffer);
        
        airlineData.setAircraft("Aircraft" + generateString(3));
        airlineData.setBookingAgencyEmail(generateEmail());

        return airlineData;
    }

    /**
     * Generates two different airport codes to ensure realistic flight data.
     * 
     * @return An array with [departure, arrival] airport codes
     */
    private static String[] generateDifferentAirportCodes() {
        String departure = generateAirportCode();
        String arrival;
        
        // Ensure departure and arrival are different
        do {
            arrival = generateAirportCode();
        } while (arrival.equals(departure));
        
        return new String[]{departure, arrival};
    }
}
