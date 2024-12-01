from pyflink.datastream.state import ValueStateDescriptor
from pyflink.datastream.functions import ProcessWindowFunction, RuntimeContext
from typing import Iterable

from model.flight_data import FlyerStatsData

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


class FlyerStatsProcessWindowFunction(ProcessWindowFunction):
    """This class is a custom implementation of a ProcessWindowFunction in Apache Flink.
    This class is designed to process elements within a window, manage state, and yield
    accumulated statistics for user data.
    """
    def __init__(self):
        """The purpose of this constructor is to set up the initial state of the instance by
        defining the state_descriptor attribute and initializing it to None.
        """
        self.state_descriptor = None

    def open(self, context: RuntimeContext):
        """The purpose of this open method is to set up the state descriptor that will be used
        later in the process method to manage state. By defining the state descriptor in the
        open method, the state is correctly initialized and available for use when processing
        elements.

        Args:
            context (RuntimeContext): An instance of the RuntimeContext class, which provides
            information about the runtime environment in which the function is executed.  This
            context can be used to access various runtime features, such as state, metrics, and
            configuration.
        """
        self.state_descriptor = ValueStateDescriptor("User Statistics Data", FlyerStatsData.get_value_type_info())

    def process(self, key: str, context: "ProcessWindowFunction.Context", elements: Iterable[FlyerStatsData]) -> Iterable:
        """This method processes elements within a window, updates the state, and yields
        the accumulated statistics.

        Args:
            key (str): Represents the key for the current group of elements.  This is useful
            for operations that need to be performed on a per-key basis.
            context (ProcessWindowFunction.Context): Provides access to the window's metadata,
            state, and timers.
            elements (Iterable[FlyerStatsData]): An iterable collection of `FlyerStatsData`
            objects that belong to the current window and key.  The method processes these elements
            to compute the result.

        Yields:
            Iterable: yeilds the accumulated statistics as a `FlyerStatsData` object.
        """
        # Retrieves the state associated with the current window
        state = context.global_state().get_state(self.state_descriptor)

        # Get's the current state
        accumulated_stats = state.value()

        # Iterates over the elements in the window
        for new_stats in elements:
            if accumulated_stats is None:
                # Initializes the first element's row representation
                accumulated_stats = new_stats.to_row()
            else:
                # Merges the current `accumulated_stats` with the new element's statistics
                # and updates the state
                accumulated_stats = FlyerStatsData.merge(FlyerStatsData.from_row(accumulated_stats), new_stats).to_row()

        # Updates the state with the new accumulated statistics
        state.update(accumulated_stats)

        # Yields the accumulated statistics as a `FlyerStatsData` object
        yield FlyerStatsData.from_row(accumulated_stats)
