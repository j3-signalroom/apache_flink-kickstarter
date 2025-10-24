/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.junit5.MiniClusterExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import kickstarter.model.AirlineJsonData;
import kickstarter.model.FlightJsonData;


/**
 * Unit tests for JsonFlightConsolidatorApp.
 * Tests the workflow that consolidates flight data from SkyOne and Sunset airlines,
 * filtering out past flights and converting to a unified FlightJsonData format.
 */
class JsonFlightConsolidatorAppTest {

    StreamExecutionEnvironment env;
    DataStream.Collector<FlightJsonData> collector;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    static final MiniClusterResourceConfiguration MINI_CLUSTER_CONFIG = new MiniClusterResourceConfiguration.Builder()
            .setNumberSlotsPerTaskManager(2)
            .setNumberTaskManagers(1)
            .build();

    @RegisterExtension
    static final MiniClusterExtension FLINK = new MiniClusterExtension(MINI_CLUSTER_CONFIG);

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
        final String futureArrivalTime = LocalDateTime.now().plusMinutes(1).format(DATE_TIME_FORMATTER);
        final String pastArrivalTime = LocalDateTime.now().minusSeconds(1).format(DATE_TIME_FORMATTER);

        AirlineJsonData newSkyOneFlight = new JsonTestHelpers.AirlineFlightDataBuilder().setArrivalTime(futureArrivalTime).build();
        AirlineJsonData oldSkyOneFlight = new JsonTestHelpers.AirlineFlightDataBuilder().setArrivalTime(pastArrivalTime).build();
        AirlineJsonData newSunsetFlight = new JsonTestHelpers.AirlineFlightDataBuilder().setArrivalTime(futureArrivalTime).build();
        AirlineJsonData oldSunsetFlight = new JsonTestHelpers.AirlineFlightDataBuilder().setArrivalTime(pastArrivalTime).build();

        DataStreamSource<AirlineJsonData> skyOneStream = env.fromData(newSkyOneFlight, oldSkyOneFlight);
        DataStreamSource<AirlineJsonData> sunsetStream = env.fromData(newSunsetFlight, oldSunsetFlight);

        JsonFlightConsolidatorApp.defineWorkflow(skyOneStream, sunsetStream).collectAsync(collector);

        env.executeAsync();

        assertContains(collector, Arrays.asList(newSkyOneFlight.toFlightData("SkyOne"), newSunsetFlight.toFlightData("Sunset")));
    }
}