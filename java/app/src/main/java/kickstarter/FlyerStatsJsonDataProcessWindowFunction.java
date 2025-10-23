/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import kickstarter.model.FlyerStatsJsonData;


/**
 * Window processing function that accumulates flyer statistics across time windows.
 * Maintains global state to track cumulative statistics (total flight duration and
 * number of flights) for each user across all processing windows.
 * This version works with JSON-serialized data.
 */
class FlyerStatsJsonDataProcessWindowFunction extends ProcessWindowFunction<FlyerStatsJsonData, FlyerStatsJsonData, String, TimeWindow> {
    // --- Best practice for Serialization classes in Flink: define a serial version UID for serialization
    private static final long serialVersionUID = 1L;

    // --- State Descriptor should not be serialized; they're recreated in open()
    private transient ValueStateDescriptor<FlyerStatsJsonData> stateDescriptor;

        /**
     * Initializes the state descriptor. This method is called once when the function
     * is instantiated, ensuring optimal performance by avoiding repeated descriptor creation.
     * 
     * @param parameters The configuration parameters for this function
     * @throws Exception if the initialization fails
     */
    @Override
    public void open(OpenContext openContext) throws Exception {
        super.open(openContext);
        stateDescriptor = new ValueStateDescriptor<>("user_statistics", FlyerStatsJsonData.class);
    }

    /**
     * The process method is called for each window, and it processes the elements in the window.  It instantiates a new ValueStateDescriptor
     * to create a ValueState object for the function.  The ValueState object is used to store a single value for
     * each key in a keyed stream.  The state is used to store the accumulated statistics for the user.  The state is 
     * updated each time the function processes a new element.
     * 
     * @param emailAddress The email address of the user.
     * @param context The context for this window.
     * @param statsList The list of statistics for the user.
     * @param collector The collector for emitting results.
     * @throws Exception - Implementations may forward exceptions, which are caught
     */
    @Override
    public void process(String emailAddress, Context context, Iterable<FlyerStatsJsonData> statsList, Collector<FlyerStatsJsonData> collector) throws Exception {
        // --- Retrieve the state that is persisted across windows
        ValueState<FlyerStatsJsonData> state = 
            context
                .globalState()
                .getState(stateDescriptor);

        // --- Get the accumulated stats
        FlyerStatsJsonData accumulatedStats = state.value();

        // --- Merge the stats
        for (FlyerStatsJsonData newStats: statsList) {
            if(accumulatedStats == null) {
                // --- This is the first time we've seen this flyer stats
                accumulatedStats = new FlyerStatsJsonData();
                accumulatedStats.setEmailAddress(emailAddress);
                accumulatedStats.setTotalFlightDuration(newStats.getTotalFlightDuration());
                accumulatedStats.setNumberOfFlights(newStats.getNumberOfFlights());
            } else {
                // --- Merge with existing accumulated stats
                accumulatedStats = merge(accumulatedStats, newStats);
            }
        }

        // --- Update the stored state
        state.update(accumulatedStats);

        // --- Emit the accumulated stats
        collector.collect(accumulatedStats);
    }

    /**
     * Merges two FlyerStatsJsonData objects by summing their flight durations and flight counts.
     * Creates a new object to avoid mutating the state directly.
     * 
     * @param existing The existing accumulated statistics
     * @param newStats The new statistics to merge
     * @return A new FlyerStatsJsonData object with the merged statistics
     */
    private FlyerStatsJsonData merge(FlyerStatsJsonData existing, FlyerStatsJsonData newStats) {
        FlyerStatsJsonData merged = new FlyerStatsJsonData();
        merged.setEmailAddress(existing.getEmailAddress());
        merged.setTotalFlightDuration(existing.getTotalFlightDuration() + newStats.getTotalFlightDuration());
        merged.setNumberOfFlights(existing.getNumberOfFlights() + newStats.getNumberOfFlights());
        return merged;
    }    
}