/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.junit5.MiniClusterExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kickstarter.model.FlightJsonData;
import kickstarter.model.FlyerStatsJsonData;


/**
 * Unit tests for JsonFlyerStatsApp.
 * Tests the workflow that aggregates flight data into flyer statistics,
 * grouped by email address and windowed by minute.
 */
class JsonFlyerStatsAppTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonFlyerStatsAppTest.class);

    StreamExecutionEnvironment env;
    WatermarkStrategy<FlightJsonData> defaultWatermarkStrategy;

    DataStream.Collector<FlyerStatsJsonData> collector;

    private static final long BASE_TIMESTAMP = System.currentTimeMillis();

    static final MiniClusterResourceConfiguration MINI_CLUSTER_CONFIG = new MiniClusterResourceConfiguration.Builder()
            .setNumberSlotsPerTaskManager(2)
            .setNumberTaskManagers(1)
            .build();

    @RegisterExtension
    static final MiniClusterExtension FLINK = new MiniClusterExtension(MINI_CLUSTER_CONFIG);

    private void assertContains(DataStream.Collector<FlyerStatsJsonData> collector, List<FlyerStatsJsonData> expected) {
        List<FlyerStatsJsonData> actual = new ArrayList<>();
        
        try {
            collector.getOutput().forEachRemaining(actual::add);
        } catch (Exception e) {
            LOGGER.error("Failed to collect results", e);
            fail("Failed to collect results: " + e.getMessage());
        }

        // --- Detailed assertions with informative error messages
        assertEquals(expected.size(), actual.size(), String.format("Expected %d flyer stats but got %d. Actual: %s", expected.size(), actual.size(), actual));

        // --- Check both directions for comprehensive validation
        for (FlyerStatsJsonData expectedStats : expected) {
            assertTrue(actual.contains(expectedStats), String.format("Expected flyer stats not found: %s. Actual stats: %s", expectedStats, actual));
        }
        for (FlyerStatsJsonData actualStats : actual) {
            assertTrue(expected.contains(actualStats), String.format("Unexpected flyer stats found: %s. Expected stats: %s", actualStats, expected));
        }
    }

    @BeforeEach
    public void setup() {
        env = StreamExecutionEnvironment.getExecutionEnvironment();
        defaultWatermarkStrategy = WatermarkStrategy
                .<FlightJsonData>forMonotonousTimestamps()
                .withTimestampAssigner((event, timestamp) -> BASE_TIMESTAMP);

        collector = new DataStream.Collector<>();
    }

    @Test
    void defineWorkflow_shouldConvertFlightDataToFlyerStatsData() throws Exception {
        FlightJsonData flight = new JsonTestHelpers.FlightDataBuilder().build();
        DataStream<FlightJsonData> stream = env.fromData(flight).assignTimestampsAndWatermarks(defaultWatermarkStrategy);
        JsonFlyerStatsApp.defineWorkflow(stream).collectAsync(collector);
        env.executeAsync();
        FlyerStatsJsonData expected = new FlyerStatsJsonData(flight);
        assertContains(collector, Arrays.asList(expected));
    }

    @Test
    void defineWorkflow_shouldGroupStatisticsByEmailAddress() throws Exception {
        String email1 = JsonTestHelpers.generateEmail();
        String email2 = JsonTestHelpers.generateEmail();

        FlightJsonData flight1 = new JsonTestHelpers.FlightDataBuilder().setEmailAddress(email1).build();
        FlightJsonData flight2 = new JsonTestHelpers.FlightDataBuilder().setEmailAddress(email2).build();
        FlightJsonData flight3 = new JsonTestHelpers.FlightDataBuilder().setEmailAddress(email1).build();

        DataStream<FlightJsonData> stream = env
                .fromData(flight1, flight2, flight3)
                .assignTimestampsAndWatermarks(defaultWatermarkStrategy);

        JsonFlyerStatsApp
                .defineWorkflow(stream)
                .collectAsync(collector);

        env.executeAsync();

        FlyerStatsJsonData expected1 = new FlyerStatsJsonData(flight1).merge(new FlyerStatsJsonData(flight3));
        FlyerStatsJsonData expected2 = new FlyerStatsJsonData(flight2);

        assertContains(collector, Arrays.asList(expected1, expected2));
    }

    @Test
    void defineWorkflow_shouldWindowStatisticsByMinute() throws Exception {
        String email = JsonTestHelpers.generateEmail();
        FlightJsonData flight1 = new JsonTestHelpers.FlightDataBuilder().setEmailAddress(email).build();
        FlightJsonData flight2 = new JsonTestHelpers.FlightDataBuilder().setEmailAddress(email).build();
        FlightJsonData flight3 = new JsonTestHelpers.FlightDataBuilder().setEmailAddress(email).setDepartureAirportCode("LATE")
                .build();

        WatermarkStrategy<FlightJsonData> watermarkStrategy = WatermarkStrategy
                .<FlightJsonData>forMonotonousTimestamps()
                .withTimestampAssigner((event, timestamp) -> {
                    if (event.getDepartureAirportCode().equals("LATE")) {
                        return System.currentTimeMillis() + Duration.ofMinutes(1).toMillis();
                    } else {
                        return System.currentTimeMillis();
                    }
                });

        DataStream<FlightJsonData> stream = env
                .fromData(flight1, flight2, flight3)
                .assignTimestampsAndWatermarks(watermarkStrategy);

        JsonFlyerStatsApp
                .defineWorkflow(stream)
                .collectAsync(collector);

        env.executeAsync();

        FlyerStatsJsonData expected1 = new FlyerStatsJsonData(flight1).merge(new FlyerStatsJsonData(flight2));
        FlyerStatsJsonData expected2 = expected1.merge(new FlyerStatsJsonData(flight3));

        assertContains(collector, Arrays.asList(expected1, expected2));
    }
}