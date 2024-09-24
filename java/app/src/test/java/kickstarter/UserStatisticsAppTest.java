/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

import kickstarter.model.*;


class UserStatisticsAppTest {

    StreamExecutionEnvironment env;
    WatermarkStrategy<FlightData> defaultWatermarkStrategy;

    DataStream.Collector<UserStatisticsData> collector;

    static final MiniClusterResourceConfiguration miniClusterConfig = new MiniClusterResourceConfiguration.Builder()
            .setNumberSlotsPerTaskManager(2)
            .setNumberTaskManagers(1)
            .build();

    @RegisterExtension
    static final MiniClusterExtension FLINK = new MiniClusterExtension(miniClusterConfig);

    private void assertContains(DataStream.Collector<UserStatisticsData> collector, List<UserStatisticsData> expected) {
        List<UserStatisticsData> actual = new ArrayList<>();
        collector.getOutput().forEachRemaining(actual::add);

        assertEquals(expected.size(), actual.size());

        assertTrue(actual.containsAll(expected));
    }

    @BeforeEach
    public void setup() {
        env = StreamExecutionEnvironment.getExecutionEnvironment();
        defaultWatermarkStrategy = WatermarkStrategy
                .<FlightData>forMonotonousTimestamps()
                .withTimestampAssigner((event, timestamp) -> System.currentTimeMillis());

        collector = new DataStream.Collector<>();
    }

    @Test
    void defineWorkflow_shouldConvertFlightDataToUserStatisticsData() throws Exception {
        FlightData flight = new TestHelpers.FlightDataBuilder().build();

        DataStream<FlightData> stream = env
                .fromData(flight)
                .assignTimestampsAndWatermarks(defaultWatermarkStrategy);

        UserStatisticsApp
                .defineWorkflow(stream)
                .collectAsync(collector);

        env.executeAsync();

        UserStatisticsData expected = new UserStatisticsData(flight);

        assertContains(collector, Arrays.asList(expected));
    }

    @Test
    void defineWorkflow_shouldGroupStatisticsByEmailAddress() throws Exception {
        String email1 = TestHelpers.generateEmail();
        String email2 = TestHelpers.generateEmail();

        FlightData flight1 = new TestHelpers.FlightDataBuilder().setEmailAddress(email1).build();
        FlightData flight2 = new TestHelpers.FlightDataBuilder().setEmailAddress(email2).build();
        FlightData flight3 = new TestHelpers.FlightDataBuilder().setEmailAddress(email1).build();

        DataStream<FlightData> stream = env
                .fromData(flight1, flight2, flight3)
                .assignTimestampsAndWatermarks(defaultWatermarkStrategy);

        UserStatisticsApp
                .defineWorkflow(stream)
                .collectAsync(collector);

        env.executeAsync();

        UserStatisticsData expected1 = new UserStatisticsData(flight1).merge(new UserStatisticsData(flight3));
        UserStatisticsData expected2 = new UserStatisticsData(flight2);

        assertContains(collector, Arrays.asList(expected1, expected2));
    }

    @Test
    void defineWorkflow_shouldWindowStatisticsByMinute() throws Exception {
        String email = TestHelpers.generateEmail();
        FlightData flight1 = new TestHelpers.FlightDataBuilder().setEmailAddress(email).build();
        FlightData flight2 = new TestHelpers.FlightDataBuilder().setEmailAddress(email).build();
        FlightData flight3 = new TestHelpers.FlightDataBuilder().setEmailAddress(email).setDepartureAirportCodeCode("LATE")
                .build();

        WatermarkStrategy<FlightData> watermarkStrategy = WatermarkStrategy
                .<FlightData>forMonotonousTimestamps()
                .withTimestampAssigner((event, timestamp) -> {
                    if (event.getDepartureAirportCodeCode().equals("LATE")) {
                        return System.currentTimeMillis() + Duration.ofMinutes(1).toMillis();
                    } else {
                        return System.currentTimeMillis();
                    }
                });

        DataStream<FlightData> stream = env
                .fromData(flight1, flight2, flight3)
                .assignTimestampsAndWatermarks(watermarkStrategy);

        UserStatisticsApp
                .defineWorkflow(stream)
                .collectAsync(collector);

        env.executeAsync();

        UserStatisticsData expected1 = new UserStatisticsData(flight1).merge(new UserStatisticsData(flight2));
        UserStatisticsData expected2 = expected1.merge(new UserStatisticsData(flight3));

        assertContains(collector, Arrays.asList(expected1, expected2));
    }
}