# Flink Applications Powered by Python on Confluent Cloud for Apache Flink (CCAF)

**Table of Contents**

<!-- toc -->
+ [1.0 Interact with the CCAF Table API locally](#10-interact-with-the-ccaf-table-api-locally)
+ [2.0 Discover What You Can Do with These Flink Apps](#20-discover-what-you-can-do-with-these-flink-apps)
  * [2.1 Avro formatted data](#21-avro-formatted-data)
+ [3.0 Resources](#30-resources)
<!-- tocstop -->


## 1.0 Interact with the CCAF Table API locally

1. To interact with the CCAF Table API locally, start a shell within the poetry virtualenv:

    ```bash
    poetry shell
    ```

2. Then start Python in interactive mode with CCAF Table API:

    ```bash
    python -i setup_pyshell.py
    ```

3. The `TableEnvironment` is pre-initialized from environment variables and available under `tbl_env`.

## 2.0 Discover What You Can Do with These Flink Apps

### 2.1 Avro formatted data
Flink App|Flink Run Command
-|-
**`avro_flight_consolidator_app`**|`../scripts/run-avro-flight-consolidator-ccaf-app-locally.sh --profile=<AWS_SSO_PROFILE_NAME> --service-account-user=<SERVICE_ACCOUNT_USER>`
**IN DEVELOPMENT >>>** **`avro_flyer_stats_ccaf_app`**|`../scripts/run-avro-flyer-stats-ccaf-app-locally.sh --profile=<AWS_SSO_PROFILE_NAME> --service-account-user=<SERVICE_ACCOUNT_USER>`

> Argument placeholder|Replace with
> -|-
> `<AWS_SSO_PROFILE_NAME>`|your AWS SSO profile name for your AWS infrastructue that host your AWS Secrets Manager.
> `<SERVICE_ACCOUNT_USER>`|the Snowflake service account user, that is used in the name of the AWS Secrets Manager secrets path.

## 3.0 Resources
[Table API on Confluent Cloud for Apache Flink](https://docs.confluent.io/cloud/current/flink/reference/table-api.html#table-api-on-af-long)

[Table API in Confluent Cloud for Apache Flink API Function](https://docs.confluent.io/cloud/current/flink/reference/functions/table-api-functions.html#flink-table-api-functions)

[Information Schema in Confluent Cloud for Apache Flink](https://docs.confluent.io/cloud/current/flink/reference/flink-sql-information-schema.html)
