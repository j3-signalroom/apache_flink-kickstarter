# Flink Applications Powered by Python on Confluent Cloud for Apache Flink (CCAF)

**Table of Contents**

<!-- toc -->
+ [1.0 Interact with the CCAF Table API locally](#10-interact-with-the-ccaf-table-api-locally)
+ [2.0 Resources](#20-resources)
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

## 2.0 Resources
[Table API on Confluent Cloud for Apache Flink](https://docs.confluent.io/cloud/current/flink/reference/table-api.html#table-api-on-af-long)

[Table API in Confluent Cloud for Apache Flink API Function](https://docs.confluent.io/cloud/current/flink/reference/functions/table-api-functions.html#flink-table-api-functions)

[Information Schema in Confluent Cloud for Apache Flink](https://docs.confluent.io/cloud/current/flink/reference/flink-sql-information-schema.html)
