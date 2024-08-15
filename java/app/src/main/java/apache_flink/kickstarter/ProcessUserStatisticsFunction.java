/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.kickstarter;

import org.apache.flink.api.common.state.*;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.*;

import apache_flink.kickstarter.model.*;


class ProcessUserStatisticsDataFunction extends ProcessWindowFunction<UserStatisticsData, UserStatisticsData, String, TimeWindow> {
    private ValueStateDescriptor<UserStatisticsData> stateDescriptor;

    /**
     * The open method is called when the function is first initialized.
     * 
     * @param parameters The configuration parameters for the function.
     * @throws Exception - Implementations may forward exceptions, which are caught
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        stateDescriptor = new ValueStateDescriptor<>("User Statistics", UserStatisticsData.class);
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
    public void process(String emailAddress, ProcessWindowFunction<UserStatisticsData, UserStatisticsData, String, TimeWindow>.Context context, Iterable<UserStatisticsData> statsList, Collector<UserStatisticsData> collector) throws Exception {
        // --- Retrieve the state
        ValueState<UserStatisticsData> state = context.globalState().getState(stateDescriptor);

        // --- Get the accumulated stats
        UserStatisticsData accumulatedStats = state.value();

        // --- Merge the stats
        for (UserStatisticsData newStats: statsList) {
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