/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.kickstarter.datastream_api;

import org.apache.flink.api.common.state.*;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.*;

import apache_flink.kickstarter.datastream_api.model.*;


class ProcessUserStatisticsDataFunction extends ProcessWindowFunction<UserStatisticsData, UserStatisticsData, String, TimeWindow> {
    private ValueStateDescriptor<UserStatisticsData> stateDescriptor;

    @Override
    public void open(Configuration parameters) throws Exception {
        stateDescriptor = new ValueStateDescriptor<>("User Statistics", UserStatisticsData.class);
        super.open(parameters);
    }

    @Override
    public void process(String emailAddress, ProcessWindowFunction<UserStatisticsData, UserStatisticsData, String, TimeWindow>.Context context, Iterable<UserStatisticsData> statsList, Collector<UserStatisticsData> collector) throws Exception {
        ValueState<UserStatisticsData> state = context.globalState().getState(stateDescriptor);
        UserStatisticsData accumulatedStats = state.value();

        for (UserStatisticsData newStats: statsList) {
            if(accumulatedStats == null)
                accumulatedStats = newStats;
            else
                accumulatedStats = accumulatedStats.merge(newStats);
        }

        state.update(accumulatedStats);

        collector.collect(accumulatedStats);
    }
}