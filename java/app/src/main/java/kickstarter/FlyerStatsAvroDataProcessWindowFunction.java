/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import org.apache.flink.api.common.state.*;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.*;

import kickstarter.model.*;


class FlyerStatsAvroDataProcessWindowFunction extends ProcessWindowFunction<FlyerStatsAvroData, FlyerStatsAvroData, String, TimeWindow> {
    private ValueStateDescriptor<FlyerStatsAvroData> stateDescriptor;

    /**
     * The open method is called when the function is first initialized.  It instantiates a new ValueStateDescriptor
     * to create a ValueState object for the function.  The ValueState object is used to store a single value for
     * each key in a keyed stream.  The state is used to store the accumulated statistics for the user.  The state is 
     * updated each time the function processes a new element.
     * 
     * @param parameters The configuration parameters for the function.
     * @throws Exception - Implementations may forward exceptions, which are caught
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        stateDescriptor = new ValueStateDescriptor<>("User Statistics", FlyerStatsAvroData.class);
        super.open(parameters);
    }

    /**
     * The process method is called for each window, and it processes the elements in the window.
     * 
     * @param emailAddress The email address of the user.
     * @param context The context for this window.
     * @param statsList The list of statistics for the user.
     * @param collector The collector for emitting results.
     * @throws Exception - Implementations may forward exceptions, which are caught
     */
    @Override
    public void process(String emailAddress, ProcessWindowFunction<FlyerStatsAvroData, FlyerStatsAvroData, String, TimeWindow>.Context context, Iterable<FlyerStatsAvroData> statsList, Collector<FlyerStatsAvroData> collector) throws Exception {
        // --- Retrieve the state that is persisted across windows
        ValueState<FlyerStatsAvroData> state = 
            context
                .globalState()
                .getState(stateDescriptor);

        // --- Get the accumulated stats
        FlyerStatsAvroData accumulatedStats = state.value();

        // --- Merge the stats
        for (FlyerStatsAvroData newStats: statsList) {
            if(accumulatedStats == null)
                // --- This is the first time we've seen this flyer stats
                accumulatedStats = newStats;
            else
                accumulatedStats = merge(accumulatedStats, newStats);
        }

        // --- Update the stored state
        state.update(accumulatedStats);

        // --- Emit the accumulated stats
        collector.collect(accumulatedStats);
    }

    /**
     * The merge method is called to merge two FlyerStatsAvroData objects.
     * 
     * @param flyerStatsAvroData1 The first FlyerStatsAvroData object. 
     * @param flyerStatsAvroData2 The second FlyerStatsAvroData object.
     * 
     * @return The merged FlyerStatsAvroData object.
     */
    private FlyerStatsAvroData merge(FlyerStatsAvroData flyerStatsAvroData1, FlyerStatsAvroData flyerStatsAvroData2) {
        flyerStatsAvroData1.setTotalFlightDuration(flyerStatsAvroData1.getTotalFlightDuration() + flyerStatsAvroData2.getTotalFlightDuration());
        flyerStatsAvroData1.setNumberOfFlights(flyerStatsAvroData1.getNumberOfFlights() + flyerStatsAvroData2.getNumberOfFlights());

        return flyerStatsAvroData1;
    }
}