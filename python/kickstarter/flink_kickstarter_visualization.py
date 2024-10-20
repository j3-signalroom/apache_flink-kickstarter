from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table import TableEnvironment, EnvironmentSettings, StreamTableEnvironment
from pyflink.table.catalog import ObjectPath
import argparse
import pandas as pd
import streamlit as st
from st_aggrid import AgGrid, GridOptionsBuilder
import matplotlib.pyplot as plt

from helper.utilities import catalog_exist

__copyright__  = "Copyright (c) 2024 Jeffrey Jonathan Jennings"
__credits__    = ["Jeffrey Jonathan Jennings"]
__license__    = "MIT"
__maintainer__ = "Jeffrey Jonathan Jennings"
__email__      = "j3@thej3.com"
__status__     = "dev"


def main(args):
    """This function reads data from an Iceberg table and displays it in Streamlit.

    Args:
        args (str): is the arguments passed to the script.
    """
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

    # Create a Table Environment
    tbl_env = StreamTableEnvironment.create(stream_execution_environment=env, environment_settings=EnvironmentSettings.new_instance().in_batch_mode().build())

    # Create the Apache Iceberg catalog with integration with AWS Glue back by AWS S3
    catalog_name = "apache_kickstarter"
    bucket_name = args.s3_bucket_name.replace("_", "-") # To follow S3 bucket naming convention, replace underscores with hyphens if exist
    try:
        if not catalog_exist(tbl_env, catalog_name):
            tbl_env.execute_sql(f"""
                CREATE CATALOG {catalog_name} WITH (
                    'type' = 'iceberg',
                    'warehouse' = 's3://{bucket_name}/warehouse',
                    'catalog-impl' = 'org.apache.iceberg.aws.glue.GlueCatalog',
                    'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO',
                    'glue.skip-archive' = 'True',
                    'glue.region' = '{args.aws_region}'
                    );
            """)
        else:
            print(f"The {catalog_name} catalog already exists.")
    except Exception as e:
        print(f"A critical error occurred to during the processing of the catalog because {e}")
        exit(1)

    # Use the Iceberg catalog
    tbl_env.use_catalog(catalog_name)

    # Access the Iceberg catalog to query the airlines database
    catalog = tbl_env.get_catalog(catalog_name)

    # Print the current catalog name
    print(f"Current catalog: {tbl_env.get_current_catalog()}")

    # Check if the database exists.  If not, create it
    database_name = "airlines"
    try:
        if not catalog.database_exists(database_name):
            tbl_env.execute_sql(f"CREATE DATABASE IF NOT EXISTS {database_name};")
        else:
            print(f"The {database_name} database already exists.")
        tbl_env.use_database(database_name)
    except Exception as e:
        print(f"A critical error occurred to during the processing of the database because {e}")
        exit(1)

    # Print the current database name
    print(f"Current database: {tbl_env.get_current_database()}")

    # Set the page configuration to wide mode
    st.set_page_config(layout="wide")

    st.title("Flink Kickstarter Visualization")
    st.write("This Streamlit application displays data from the Apache Iceberg table created by the Flink Kickstarter job.")

    with st.container(border=True):    
        col1, col2 = st.columns(2)

        with col1:
            st.header("Airline Flights")

            # Bar chart of SkyOne monthly flights in 2025
            airline_monthly_flights_table = tbl_env.sql_query(f"""
                                                                select 
                                                                    extract(month from to_timestamp(departure_time)) as departure_month, 
                                                                    count(*) as flight_count
                                                                from
                                                                    airlines.flight
                                                                where
                                                                    airline = 'SkyOne' and 
                                                                    extract(year from to_timestamp(departure_time)) = 2025 
                                                                group by 
                                                                    extract(month from to_timestamp(departure_time))
                                                                order by
                                                                    departure_month asc;
                                                            """)
            df_airline_monthly_flights_table = airline_monthly_flights_table.to_pandas()
            
            fig, ax = plt.subplots()
            ax.bar(df_airline_monthly_flights_table['departure_month'], df_airline_monthly_flights_table['flight_count'])
            ax.set_xlabel('departure_month')
            ax.set_ylabel('flight_count')
            ax.set_title('SkyOne Monthly Flights in 2025')

            # Display the bar chart in Streamlit
            st.pyplot(fig)

            # Display the description of the bar chart
            st.write("This bar chart displays the number of SkyOne monthly flights in 2025.  The x-axis represents the month and the y-axis represents the number of flights.")

        with col2:
            st.header("Airport Ranking")
            # Pie chart of the top 5 airports with the most departures for SkyOne
            ranked_airports_table = tbl_env.sql_query(f"""
                                                        with cte_ranked as (
                                                            select
                                                                airline,
                                                                departure_airport_code,
                                                                flight_count,
                                                                ROW_NUMBER() OVER (PARTITION BY airline ORDER BY flight_count DESC) AS row_num
                                                            from (
                                                                select
                                                                    airline,
                                                                    departure_airport_code,
                                                                    count(*) as flight_count
                                                                from
                                                                    airlines.flight
                                                                group by
                                                                    airline,
                                                                    departure_airport_code
                                                            ) tbl
                                                        )
                                                        select 
                                                            departure_airport_code, 
                                                            flight_count
                                                        from 
                                                            cte_ranked
                                                        where 
                                                            airline = 'SkyOne' and 
                                                            row_num <= 5;
                                                    """)
            df_ranked_airports_table = ranked_airports_table.to_pandas()
            
            fig, ax = plt.subplots()
            ax.set_title('Top 5 Airports with the Most Departures for SkyOne')
            ax.pie(df_ranked_airports_table['flight_count'], labels=df_ranked_airports_table['departure_airport_code'], autopct='%1.1f%%', startangle=90)
            ax.axis('equal')  # Equal aspect ratio ensures that the pie is drawn as a circle.

            # Display the pie chart in Streamlit
            st.pyplot(fig)

            # Display the description of the pie chart
            st.write("This pie chart displays the top 5 airports with the most departures for SkyOne.  The chart shows the percentage of flights departing from each of the top 5 airports.")

    with st.container(border=True):
        st.header("Flight Data")

        # Display the flight data in a table
        flight_table = tbl_env.sql_query(f"SELECT * FROM {database_name}.flight")
        df_flight_table = flight_table.to_pandas()

        # Create grid options with only specific columns
        gb = GridOptionsBuilder.from_dataframe(df_flight_table)
        gb.configure_columns(["email_address", "departure_time", "departure_airport_code", "arrival_time", "arrival_airport_code", "flight_number", "confirmation_code", "airline"]) 
        gridOptions = gb.build()
        AgGrid(
            df_flight_table,
            gridOptions=gridOptions,
            height=300, 
            width='100%'
        )

        # Display the description of the table
        st.write("This table displays the flight data from the Apache Iceberg table.  The table shows the email address, departure time, departure airport code, arrival time, arrival airport code, flight number, confirmation code, and airline for each flight.")


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
