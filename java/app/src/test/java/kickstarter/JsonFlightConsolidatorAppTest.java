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
import java.time.*;
import java.time.format.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

import kickstarter.model.*;


class JsonFlightConsolidatorAppTest {

    StreamExecutionEnvironment env;
    DataStream.Collector<FlightJsonData> collector;

    static final MiniClusterResourceConfiguration miniClusterConfig = new MiniClusterResourceConfiguration.Builder()
            .setNumberSlotsPerTaskManager(2)
            .setNumberTaskManagers(1)
            .build();

    @RegisterExtension
    static final MiniClusterExtension FLINK = new MiniClusterExtension(miniClusterConfig);

    private void assertContains(DataStream.Collector<FlightJsonData> collector, List<FlightJsonData> expected) {
        List<FlightJsonData> actual = new ArrayList<>();
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
        AirlineJsonData skyOneFlight = new JsonTestHelpers.AirlineFlightDataBuilder().build();
        AirlineJsonData sunsetFlight = new JsonTestHelpers.AirlineFlightDataBuilder().build();

        DataStreamSource<AirlineJsonData> skyOneStream = env.fromData(skyOneFlight);
        DataStreamSource<AirlineJsonData> sunsetStream = env.fromData(sunsetFlight);

        JsonFlightConsolidatorApp.defineWorkflow(skyOneStream, sunsetStream).collectAsync(collector);
        env.executeAsync();
        assertContains(collector, Arrays.asList(skyOneFlight.toFlightData("SkyOne"), sunsetFlight.toFlightData("Sunset")));
    }

    @Test
    void defineWorkflow_shouldFilterOutFlightsInThePast() throws Exception {
        final String addMinuteToNow = LocalDateTime.now().plusMinutes(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        final String subtractSecondToNow = LocalDateTime.now().minusSeconds(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        AirlineJsonData newSkyOneFlight = new JsonTestHelpers.AirlineFlightDataBuilder().setArrivalTime(addMinuteToNow).build();
        AirlineJsonData oldSkyOneFlight = new JsonTestHelpers.AirlineFlightDataBuilder().setArrivalTime(subtractSecondToNow).build();
        AirlineJsonData newSunsetFlight = new JsonTestHelpers.AirlineFlightDataBuilder().setArrivalTime(addMinuteToNow).build();
        AirlineJsonData oldSunsetFlight = new JsonTestHelpers.AirlineFlightDataBuilder().setArrivalTime(subtractSecondToNow).build();

        DataStreamSource<AirlineJsonData> skyOneStream = env.fromData(newSkyOneFlight, oldSkyOneFlight);
        DataStreamSource<AirlineJsonData> sunsetStream = env.fromData(newSunsetFlight, oldSunsetFlight);

        JsonFlightConsolidatorApp.defineWorkflow(skyOneStream, sunsetStream).collectAsync(collector);

        env.executeAsync();

        assertContains(collector, Arrays.asList(newSkyOneFlight.toFlightData("SkyOne"), newSunsetFlight.toFlightData("Sunset")));
    }
}