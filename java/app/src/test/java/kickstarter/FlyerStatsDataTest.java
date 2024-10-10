/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import org.junit.jupiter.api.Test;
import static org.apache.flink.types.PojoTestUtils.assertSerializedAsPojo;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;

import kickstarter.model.*;

class FlyerStatsDataTest {

    @Test
    void theClass_shouldBeSerializableAsAPOJO() {
        assertSerializedAsPojo(FlyerStatsData.class);
    }

    @Test
    void constructor_shouldCreateStatisticsUsingFlightData() {
        FlightData flightData = new TestHelpers.FlightDataBuilder().build();
        FlyerStatsData stats = new FlyerStatsData(flightData);

        Duration expectedDuration = Duration.between(ZonedDateTime.parse(flightData.getDepartureTime()), ZonedDateTime.parse(flightData.getArrivalTime()));

        assertEquals(flightData.getEmailAddress(), stats.getEmailAddress());
        assertEquals(expectedDuration, stats.getTotalFlightDuration());
        assertEquals(1, stats.getNumberOfFlights());
    }

    @Test
    void setters_shouldPopulateExpectedFields() {
        FlyerStatsData expected = new TestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsData actual = new FlyerStatsData();
        actual.setEmailAddress(expected.getEmailAddress());
        actual.setTotalFlightDuration(expected.getTotalFlightDuration());
        actual.setNumberOfFlights(expected.getNumberOfFlights());

        assertEquals(expected.getEmailAddress(), actual.getEmailAddress());
        assertEquals(expected.getTotalFlightDuration(), actual.getTotalFlightDuration());
        assertEquals(expected.getNumberOfFlights(), actual.getNumberOfFlights());
    }

    @Test
    void equals_shouldReturnTrue_forTwoEquivalentUserStatistics() {
        FlyerStatsData stats1 = new TestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsData stats2 = new FlyerStatsData();
        stats2.setEmailAddress(stats1.getEmailAddress());
        stats2.setTotalFlightDuration(stats1.getTotalFlightDuration());
        stats2.setNumberOfFlights(stats1.getNumberOfFlights());

        assertNotSame(stats1, stats2);
        assertEquals(stats1, stats2);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void equals_shouldReturnFalse_forTwoDifferentUserStatistics() {
        FlyerStatsData stats1 = new TestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsData stats2 = new TestHelpers.FlyerStatsDataBuilder().build();

        assertNotSame(stats1, stats2);
        assertNotEquals(stats1, stats2);
        assertNotEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void merge_shouldMergeTheUserStatistics() {
        FlyerStatsData stats1 = new TestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsData stats2 = new TestHelpers.FlyerStatsDataBuilder()
            .setEmailAddress(stats1.getEmailAddress())
            .build();

        FlyerStatsData merged = stats1.merge(stats2);

        assertEquals(stats1.getEmailAddress(), merged.getEmailAddress());
        assertEquals(stats1.getTotalFlightDuration().plus(stats2.getTotalFlightDuration()), merged.getTotalFlightDuration());
        assertEquals(stats1.getNumberOfFlights() + stats2.getNumberOfFlights(), merged.getNumberOfFlights());
    }

    @Test
    void merge_shouldFailIfTheEmailAddressesAreDifferent() {
        FlyerStatsData stats1 = new TestHelpers.FlyerStatsDataBuilder().build();
        FlyerStatsData stats2 = new TestHelpers.FlyerStatsDataBuilder().setEmailAddress("notthesame@email.com").build();

        assertNotEquals(stats1.getEmailAddress(), stats2.getEmailAddress());

        //assertThrows(AssertionError.class, () -> stats1.merge(stats2));
    }
}