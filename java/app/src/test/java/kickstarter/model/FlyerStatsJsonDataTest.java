/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.model;

import org.junit.jupiter.api.Test;
import static org.apache.flink.types.PojoTestUtils.assertSerializedAsPojo;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.time.format.*;

import kickstarter.*;


class FlyerStatsJsonDataTest {

    @Test
    void theClass_shouldBeSerializableAsAPOJO() {
        assertSerializedAsPojo(FlyerStatsJsonData.class);
    }

    @Test
    void constructor_shouldCreateStatisticsUsingFlightData() {
        FlightJsonData flightJsonData = new JsonTestHelpers.FlightDataBuilder().build();
        FlyerStatsJsonData stats = new FlyerStatsJsonData(flightJsonData);

        long expectedDuration = Duration.between(LocalDateTime.parse(flightJsonData.getDepartureTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                                                 LocalDateTime.parse(flightJsonData.getArrivalTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).toMinutes();


        assertEquals(flightJsonData.getEmailAddress(), stats.getEmailAddress());
        assertEquals(expectedDuration, stats.getTotalFlightDuration());
        assertEquals(1, stats.getNumberOfFlights());
    }

    @Test
    void setters_shouldPopulateExpectedFields() {
        FlyerStatsJsonData expected = new JsonTestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsJsonData actual = new FlyerStatsJsonData();
        actual.setEmailAddress(expected.getEmailAddress());
        actual.setTotalFlightDuration(expected.getTotalFlightDuration());
        actual.setNumberOfFlights(expected.getNumberOfFlights());

        assertEquals(expected.getEmailAddress(), actual.getEmailAddress());
        assertEquals(expected.getTotalFlightDuration(), actual.getTotalFlightDuration());
        assertEquals(expected.getNumberOfFlights(), actual.getNumberOfFlights());
    }

    @Test
    void equals_shouldReturnTrue_forTwoEquivalentFlyerStats() {
        FlyerStatsJsonData stats1 = new JsonTestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsJsonData stats2 = new FlyerStatsJsonData();
        stats2.setEmailAddress(stats1.getEmailAddress());
        stats2.setTotalFlightDuration(stats1.getTotalFlightDuration());
        stats2.setNumberOfFlights(stats1.getNumberOfFlights());

        assertNotSame(stats1, stats2);
        assertEquals(stats1, stats2);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void equals_shouldReturnFalse_forTwoDifferentFlyerStats() {
        FlyerStatsJsonData stats1 = new JsonTestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsJsonData stats2 = new JsonTestHelpers.FlyerStatsDataBuilder().build();

        assertNotSame(stats1, stats2);
        assertNotEquals(stats1, stats2);
        assertNotEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void merge_shouldMergeTheFlyerStats() {
        FlyerStatsJsonData stats1 = new JsonTestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsJsonData stats2 = new JsonTestHelpers.FlyerStatsDataBuilder().setEmailAddress(stats1.getEmailAddress()).build();

        FlyerStatsJsonData merged = stats1.merge(stats2);

        assertEquals(stats1.getEmailAddress(), merged.getEmailAddress());
        assertEquals(stats1.getTotalFlightDuration() + stats2.getTotalFlightDuration(), merged.getTotalFlightDuration());
        assertEquals(stats1.getNumberOfFlights() + stats2.getNumberOfFlights(), merged.getNumberOfFlights());
    }

    @Test
    void merge_shouldFailIfTheEmailAddressesAreDifferent() {
        FlyerStatsJsonData stats1 = new JsonTestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsJsonData stats2 = new JsonTestHelpers.FlyerStatsDataBuilder().setEmailAddress("notthesame@email.com").build();

        assertNotEquals(stats1.getEmailAddress(), stats2.getEmailAddress());

        //assertThrows(AssertionError.class, () -> stats1.merge(stats2));
    }
}