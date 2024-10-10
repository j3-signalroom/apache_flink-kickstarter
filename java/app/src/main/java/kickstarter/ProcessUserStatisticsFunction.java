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


class ProcessUserStatisticsDataFunction extends ProcessWindowFunction<FlyerStatsData, FlyerStatsData, String, TimeWindow> {
    private ValueStateDescriptor<FlyerStatsData> stateDescriptor;

    /**
     * The open method is called when the function is first initialized.
     * 
     * @param parameters The configuration parameters for the function.
     * @throws Exception - Implementations may forward exceptions, which are caught
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        stateDescriptor = new ValueStateDescriptor<>("User Statistics", FlyerStatsData.class);
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
    public void process(String emailAddress, ProcessWindowFunction<FlyerStatsData, FlyerStatsData, String, TimeWindow>.Context context, Iterable<FlyerStatsData> statsList, Collector<FlyerStatsData> collector) throws Exception {
        // --- Retrieve the state
        ValueState<FlyerStatsData> state = context.globalState().getState(stateDescriptor);

        // --- Get the accumulated stats
        FlyerStatsData accumulatedStats = state.value();

        // --- Merge the stats
        for (FlyerStatsData newStats: statsList) {
            if(accumulatedStats == null)
                accumulatedStats = newStats;
            else
                accumulatedStats = accumulatedStats.merge(newStats);
        }

        // --- Update the state
        state.update(accumulatedStats);

        // --- Emit the accumulated stats
        collector.collect(accumulatedStats);
    }
}