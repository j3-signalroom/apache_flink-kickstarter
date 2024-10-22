from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table import TableEnvironment, EnvironmentSettings, StreamTableEnvironment
from pyflink.table.catalog import ObjectPath
import argparse
from typing import Tuple
import pandas as pd
import streamlit as st
from st_aggrid import AgGrid, GridOptionsBuilder
import plotly.express as px
import altair as alt

from helper.utilities import *

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


@st.cache_data
def load_data(_tbl_env: StreamTableEnvironment, database_name: str) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    """This function creates and loads from multiple query data results
    from the `airlines.flight` Apache Iceberg Table into cache and then
    returns the query results as Pandas DataFrames from the cache.
    
    Args:
        _tbl_env (StreamTableEnvironment): is the Table Environment.
        database_name (str): is the name of the database.
        
        Returns:
            Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]: is a tuple of Pandas DataFrames.
    """
    # Get the number of flights per month by airline, year, and month
    airline_monthly_flights_table = _tbl_env.sql_query(f"""
                                                        select 
                                                            airline,
                                                            extract(year from to_timestamp(departure_time)) as departure_year,
                                                            extract(month from to_timestamp(departure_time)) as departure_month, 
                                                            concat(date_format(to_timestamp(departure_time), 'MM'), '-', date_format(to_timestamp(departure_time), 'MMM')) as departure_month_abbr,
                                                            count(*) as flight_count
                                                        from
                                                            airlines.flight
                                                        group by 
                                                            airline,
                                                            extract(year from to_timestamp(departure_time)),
                                                            extract(month from to_timestamp(departure_time)),
                                                            concat(date_format(to_timestamp(departure_time), 'MM'), '-', date_format(to_timestamp(departure_time), 'MMM'))
                                                        order by
                                                            departure_year asc,
                                                            departure_month asc;
                                                    """)
    df_airline_monthly_flights_table = airline_monthly_flights_table.to_pandas()

    # Get the top airports with the most departures by airport, airline, year, and rank
    ranked_airports_table = _tbl_env.sql_query(f"""
                                                with cte_ranked as (
                                                    select
                                                        airline,
                                                        departure_year,
                                                        departure_airport_code,
                                                        flight_count,
                                                        ROW_NUMBER() OVER (PARTITION BY airline, departure_year ORDER BY flight_count DESC) AS row_num
                                                    from (
                                                        select
                                                            airline,
                                                            extract(year from to_timestamp(departure_time)) as departure_year,
                                                            departure_airport_code,
                                                            count(*) as flight_count
                                                        from
                                                            airlines.flight
                                                        group by
                                                            airline,
                                                            extract(year from to_timestamp(departure_time)),
                                                            departure_airport_code
                                                    ) tbl
                                                )
                                                select 
                                                    airline,
                                                    departure_year,
                                                    departure_airport_code, 
                                                    flight_count,
                                                    row_num
                                                from 
                                                    cte_ranked;
                                            """)
    df_ranked_airports_table = ranked_airports_table.to_pandas()

    # Get the flight data by airline and year
    flight_table = _tbl_env.sql_query(f"SELECT *, extract(year from to_timestamp(departure_time)) as departure_year FROM {database_name}.flight")
    df_flight_table = flight_table.to_pandas()

    return df_airline_monthly_flights_table, df_ranked_airports_table, df_flight_table


def main(args):
    """This function reads data from an Iceberg table and displays it in Streamlit.

    Args:
        args (str): is the arguments passed to the script.
    """
    # --- Set the page configuration to wide mode
    st.set_page_config(layout="wide")

    # --- Create a blank Flink execution environment
    env = StreamExecutionEnvironment.get_execution_environment()

    # --- Enable checkpointing every 5000 milliseconds (5 seconds)
    env.enable_checkpointing(5000)

    #
    # Set timeout to 60 seconds
    # The maximum amount of time a checkpoint attempt can take before being discarded.
    #
    env.get_checkpoint_config().set_checkpoint_timeout(60000)

    #
    # Set the maximum number of concurrent checkpoints to 1 (i.e., only one checkpoint
    # is created at a time)
    #
    env.get_checkpoint_config().set_max_concurrent_checkpoints(1)

    # --- Add the Python dependency script files to the environment
    env.add_python_archive("/opt/flink/python_apps/kickstarter/python_files.zip")

    # --- Create a Table Environment
    tbl_env = StreamTableEnvironment.create(stream_execution_environment=env, environment_settings=EnvironmentSettings.new_instance().in_batch_mode().build())

    # --- Load Apache Iceberg catalog
    catalog = load_catalog(tbl_env, args.aws_region, args.s3_bucket_name.replace("_", "-"), "apache_kickstarter")

    # --- Print the current catalog name
    print(f"Current catalog: {tbl_env.get_current_catalog()}")

    # --- Load database
    load_database(tbl_env, catalog, "airlines")

    # Print the current database name
    print(f"Current database: {tbl_env.get_current_database()}")

    # Load the data
    df_airline_monthly_flights_table, df_ranked_airports_table, df_flight_table = load_data(tbl_env, tbl_env.get_current_database())

    st.title("Apache Flink Kickstarter Dashboard")
    st.write("This Streamlit application displays data from the Apache Iceberg table created by the DataGenerator and FlightImporter Flink Apps.")

    # Create a dropdown boxes
    selected_airline = st.selectbox(
        index=0, 
        label='Choose Airline:',
        options=df_flight_table['airline'].dropna().unique()
    )
    selected_departure_year = st.selectbox(
        index=0,
        label='Choose Depature Year:',
        options=df_flight_table['departure_year'].dropna().unique()
    )

    with st.container(border=True):    
        col1, col2 = st.columns(2)

        with col1:
            st.header("Airline Flights")

            # Bar chart
            st.title(f"{selected_airline} Monthly Flights in {selected_departure_year}")
            st.bar_chart(data=df_airline_monthly_flights_table[(df_airline_monthly_flights_table['departure_year'] == selected_departure_year) & (df_airline_monthly_flights_table['airline'] == selected_airline)] ,
                         x="departure_month_abbr",
                         y="flight_count",
                         x_label="Departure Month",
                         y_label="Number of Flights")
                        
            # Display the description of the bar chart
            st.write(f"This bar chart displays the number of {selected_airline} monthly flights in {selected_departure_year}.  The x-axis represents the month and the y-axis represents the number of flights.")

        with col2:
            st.header("Airport Ranking")
            st.title(f"Top {selected_departure_year} {selected_airline} Airports")

            # Filter the ranked airports table
            df_filter_table = df_ranked_airports_table[(df_ranked_airports_table['airline'] == selected_airline) & (df_ranked_airports_table['departure_year'] == selected_departure_year)]

            # Create a slider to select the number of airports to rank
            rank_value = st.slider(label="Ranking:",
                                   min_value=3,
                                   max_value=df_filter_table['row_num'].max(), 
                                   step=1,
                                   value=3)

            # Pie chart
            fig = px.pie(df_filter_table[(df_filter_table['row_num'] <= rank_value)], 
                         values='flight_count', 
                         names='departure_airport_code', 
                         title=f"Top {rank_value} based on departures",)
            st.plotly_chart(fig, theme=None)

            # Display the description of the pie chart
            st.write(f"This pie chart displays the top {rank_value} airports with the most departures for {selected_airline}.  The chart shows the percentage of flights departing from each of the top {rank_value} airports.")

    with st.container(border=True):
        st.header(f"{selected_departure_year} {selected_airline} Flight Data")

        # Create grid options with only specific columns
        df_filter_table = df_flight_table[(df_flight_table['departure_year'] == selected_departure_year) & (df_flight_table['airline'] == selected_airline)]
        AgGrid(
            df_filter_table,
            gridOptions=GridOptionsBuilder.from_dataframe(df_flight_table).build(),
            height=300, 
            width='100%'
        )

        # Display the description of the table
        st.write("This table displays the flight data from the Apache Iceberg table.  The table shows the email address, departure time, departure airport code, arrival time, arrival airport code, flight number, confirmation code, airline, and departure year for each flight.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--aws-s3-bucket',
                        dest='s3_bucket_name',
                        required=True,
                        help='The AWS S3 bucket name.')
    parser.add_argument('--aws-region',
                        dest='aws_region',
                        required=True,
                        help='The AWS Rgion name.')
    known_args, _ = parser.parse_known_args()
    main(known_args)
