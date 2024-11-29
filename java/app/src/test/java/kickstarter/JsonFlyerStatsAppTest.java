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


class JsonFlyerStatsAppTest {

    StreamExecutionEnvironment env;
    WatermarkStrategy<FlightJsonData> defaultWatermarkStrategy;

    DataStream.Collector<FlyerStatsJsonData> collector;

    static final MiniClusterResourceConfiguration miniClusterConfig = new MiniClusterResourceConfiguration.Builder()
            .setNumberSlotsPerTaskManager(2)
            .setNumberTaskManagers(1)
            .build();

    @RegisterExtension
    static final MiniClusterExtension FLINK = new MiniClusterExtension(miniClusterConfig);

    private void assertContains(DataStream.Collector<FlyerStatsJsonData> collector, List<FlyerStatsJsonData> expected) {
        List<FlyerStatsJsonData> actual = new ArrayList<>();
        collector.getOutput().forEachRemaining(actual::add);

        assertEquals(expected.size(), actual.size());

        assertTrue(actual.containsAll(expected));
    }

    @BeforeEach
    public void setup() {
        env = StreamExecutionEnvironment.getExecutionEnvironment();
        defaultWatermarkStrategy = WatermarkStrategy
                .<FlightJsonData>forMonotonousTimestamps()
                .withTimestampAssigner((event, timestamp) -> System.currentTimeMillis());

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