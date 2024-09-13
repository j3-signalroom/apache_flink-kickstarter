/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import java.time.ZonedDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

import kickstarter.model.*;


class FlightImporterAppTest {

    StreamExecutionEnvironment env;
    DataStream.Collector<FlightData> collector;

    static final MiniClusterResourceConfiguration miniClusterConfig = new MiniClusterResourceConfiguration.Builder()
            .setNumberSlotsPerTaskManager(2)
            .setNumberTaskManagers(1)
            .build();

    @RegisterExtension
    static final MiniClusterExtension FLINK = new MiniClusterExtension(miniClusterConfig);

    private void assertContains(DataStream.Collector<FlightData> collector, List<FlightData> expected) {
        List<FlightData> actual = new ArrayList<>();
        collector.getOutput().forEachRemaining(actual::add);

        assertEquals(expected.size(), actual.size());

        assertTrue(actual.containsAll(expected));
    }

    @BeforeEach
    void setup() {
        env = StreamExecutionEnvironment.getExecutionEnvironment();
        collector = new DataStream.Collector<>();
    }

    @Test
    void defineWorkflow_shouldConvertDataFromTwoStreams() throws Exception {
        SkyOneAirlinesFlightData skyOneFlight = new TestHelpers.SkyOneBuilder().build();
        SunsetAirFlightData sunsetFlight = new TestHelpers.SunsetBuilder().build();

        DataStreamSource<SkyOneAirlinesFlightData> skyOneStream = env.fromData(skyOneFlight);
        DataStreamSource<SunsetAirFlightData> sunsetStream = env.fromData(sunsetFlight);

        FlightImporterApp
                .defineWorkflow(skyOneStream, sunsetStream)
                .collectAsync(collector);

        env.executeAsync();

        assertContains(collector, Arrays.asList(skyOneFlight.toFlightData(), sunsetFlight.toFlightData()));
    }

    @Test
    void defineWorkflow_shouldFilterOutFlightsInThePast() throws Exception {
        SkyOneAirlinesFlightData newSkyOneFlight = new TestHelpers.SkyOneBuilder()
                .setFlightArrivalTime(ZonedDateTime.now().plusMinutes(1))
                .build();
        SkyOneAirlinesFlightData oldSkyOneFlight = new TestHelpers.SkyOneBuilder()
                .setFlightArrivalTime(ZonedDateTime.now().minusSeconds(1))
                .build();

        SunsetAirFlightData newSunsetFlight = new TestHelpers.SunsetBuilder()
                .setArrivalTime(ZonedDateTime.now().plusMinutes(1))
                .build();
        SunsetAirFlightData oldSunsetFlight = new TestHelpers.SunsetBuilder()
                .setArrivalTime(ZonedDateTime.now().minusSeconds(1))
                .build();

        DataStreamSource<SkyOneAirlinesFlightData> skyOneStream = env.fromData(newSkyOneFlight, oldSkyOneFlight);
        DataStreamSource<SunsetAirFlightData> sunsetStream = env.fromData(newSunsetFlight, oldSunsetFlight);

        FlightImporterApp
                .defineWorkflow(skyOneStream, sunsetStream)
                .collectAsync(collector);

        env.executeAsync();

        assertContains(collector, Arrays.asList(newSkyOneFlight.toFlightData(), newSunsetFlight.toFlightData()));
    }
}