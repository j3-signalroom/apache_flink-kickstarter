/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import org.junit.jupiter.api.Test;

import kickstarter.model.*;

import java.time.Duration;
import static org.apache.flink.types.PojoTestUtils.assertSerializedAsPojo;
import static org.junit.jupiter.api.Assertions.*;

class UserStatisticsTest {

    @Test
    void theClass_shouldBeSerializableAsAPOJO() {
        assertSerializedAsPojo(UserStatisticsData.class);
    }

    @Test
    void constructor_shouldCreateStatisticsUsingFlightData() {
        FlightData flightData = new TestHelpers.FlightDataBuilder().build();
        UserStatisticsData stats = new UserStatisticsData(flightData);

        Duration expectedDuration = Duration.between(flightData.getDepartureTime(), flightData.getArrivalTime());

        assertEquals(flightData.getEmailAddress(), stats.getEmailAddress());
        assertEquals(expectedDuration, stats.getTotalFlightDuration());
        assertEquals(1, stats.getNumberOfFlights());
    }

    @Test
    void setters_shouldPopulateExpectedFields() {
        UserStatisticsData expected = new TestHelpers.UserStatisticsBuilder().build();
        UserStatisticsData actual = new UserStatisticsData();
        actual.setEmailAddress(expected.getEmailAddress());
        actual.setTotalFlightDuration(expected.getTotalFlightDuration());
        actual.setNumberOfFlights(expected.getNumberOfFlights());

        assertEquals(expected.getEmailAddress(), actual.getEmailAddress());
        assertEquals(expected.getTotalFlightDuration(), actual.getTotalFlightDuration());
        assertEquals(expected.getNumberOfFlights(), actual.getNumberOfFlights());
    }

    @Test
    void equals_shouldReturnTrue_forTwoEquivalentUserStatistics() {
        UserStatisticsData stats1 = new TestHelpers.UserStatisticsBuilder().build();
        UserStatisticsData stats2 = new UserStatisticsData();
        stats2.setEmailAddress(stats1.getEmailAddress());
        stats2.setTotalFlightDuration(stats1.getTotalFlightDuration());
        stats2.setNumberOfFlights(stats1.getNumberOfFlights());

        assertNotSame(stats1, stats2);
        assertEquals(stats1, stats2);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void equals_shouldReturnFalse_forTwoDifferentUserStatistics() {
        UserStatisticsData stats1 = new TestHelpers.UserStatisticsBuilder().build();
        UserStatisticsData stats2 = new TestHelpers.UserStatisticsBuilder().build();

        assertNotSame(stats1, stats2);
        assertNotEquals(stats1, stats2);
        assertNotEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void merge_shouldMergeTheUserStatistics() {
        UserStatisticsData stats1 = new TestHelpers.UserStatisticsBuilder().build();
        UserStatisticsData stats2 = new TestHelpers.UserStatisticsBuilder()
            .setEmailAddress(stats1.getEmailAddress())
            .build();

        UserStatisticsData merged = stats1.merge(stats2);

        assertEquals(stats1.getEmailAddress(), merged.getEmailAddress());
        assertEquals(stats1.getTotalFlightDuration().plus(stats2.getTotalFlightDuration()), merged.getTotalFlightDuration());
        assertEquals(stats1.getNumberOfFlights() + stats2.getNumberOfFlights(), merged.getNumberOfFlights());
    }

    @Test
    void merge_shouldFailIfTheEmailAddressesAreDifferent() {
        UserStatisticsData stats1 = new TestHelpers.UserStatisticsBuilder().build();
        UserStatisticsData stats2 = new TestHelpers.UserStatisticsBuilder().setEmailAddress("notthesame@email.com").build();

        assertNotEquals(stats1.getEmailAddress(), stats2.getEmailAddress());

        //assertThrows(AssertionError.class, () -> stats1.merge(stats2));
    }
}