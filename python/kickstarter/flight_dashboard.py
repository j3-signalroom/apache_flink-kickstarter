import streamlit as st
from pyiceberg.catalog import load_catalog
from pyiceberg.io.pyarrow import project_table
import s3fs
import pandas as pd
import os
import argparse

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

    s3 = s3fs.S3FileSystem(
        key=os.environ['AWS_ACCESS_KEY_ID'],
        secret=os.environ['AWS_SECRET_ACCESS_KEY'],
        client_kwargs={'region_name': os.environ['AWS_REGION']}
    )

    # Load the catalog
    catalog_name = "apache_kickstarter"
    bucket_name = args.s3_bucket_name.replace("_", "-") # To follow S3 bucket naming convention, replace underscores with hyphens if exist
    catalog = load_catalog(catalog_name, {
        'type': 'hadoop',
        'warehouse': f"'s3a://{bucket_name}/warehouse'",
        'fs': s3  # Pass the S3 filesystem
    })

    # Load the table
    table = catalog.load_table('apache_kickstarter.flight')

    # Initialize a list to collect DataFrames
    dataframes = []

    # Scan and read data
    scan = table.scan()
    for task in scan.plan_files():
        # Project the table and convert to a Pandas DataFrame
        projected = project_table(task, table.schema)
        dataframes.append(projected.to_pandas())

    # Combine all DataFrames into one
    df = pd.concat(dataframes, ignore_index=True)

    # Display the DataFrame in Streamlit
    st.dataframe(df)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--aws-s3-bucket',
                        dest='s3_bucket_name',
                        required=True,
                        help='The AWS S3 bucket name.')
    known_args, _ = parser.parse_known_args()
    main(known_args)
