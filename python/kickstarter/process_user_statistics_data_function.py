from pyflink.datastream.state import ValueStateDescriptor
from pyflink.datastream.functions import ProcessWindowFunction

from model.user_statistics_data import UserStatisticsData

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


class ProcessUserStatisticsDataFunction(ProcessWindowFunction):
    def __init__(self):
        super(ProcessUserStatisticsDataFunction, self).__init__()
        self.state_descriptor = ValueStateDescriptor("User Statistics", UserStatisticsData)

    def open(self, parameters):
        """
        The open method is called when the function is first initialized.
        
        :param parameters: The configuration parameters for the function.
        :raises Exception: Implementations may forward exceptions, which are caught.
        """
        super(ProcessUserStatisticsDataFunction, self).open(parameters)

    def process(self, email_address, context: ProcessWindowFunction.Context, stats_list, collector):
        """
        The process method is called for each window, and it processes the elements in the window.
        
        :param email_address: The email address of the user.
        :param context: The context for this window.
        :param stats_list: The list of statistics for the user.
        :param collector: The collector for emitting results.
        :raises Exception: Implementations may forward exceptions, which are caught.
        """
        # Retrieve the state
        state = context.global_state().get_state(self.state_descriptor)

        # Get the accumulated stats
        accumulated_stats = state.value()

        # Merge the stats
        for new_stats in stats_list:
            if accumulated_stats is None:
                accumulated_stats = new_stats
            else:
                accumulated_stats = accumulated_stats.merge(new_stats)

        # Update the state
        state.update(accumulated_stats)

        # Emit the accumulated stats
        collector.collect(accumulated_stats)
