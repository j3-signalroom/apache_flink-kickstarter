# Flink Applications Powered by Python on Confluent Cloud for Apache Flink (CCAF)
[Confluent Cloud for Apache Flink (CCAF)](https://docs.confluent.io/cloud/current/flink/overview.html) integrates Confluent Cloud, a fully managed Apache Kafka service, with Apache Flink, a powerful stream processing framework. This integration enables real-time data processing, analytics, and complex event processing on data streams managed by Confluent Cloud.  Your Kafka topics appear automatically as queryable Flink tables, with schemas and metadata attached by Confluent Cloud.

![flink-kafka-ecosystem](../.blog/images/flink-kafka-ecosystem.png)

Confluent Cloud for Apache Flink supports creating stream-processing applications by using Flink SQL, the [Flink Table API](https://docs.confluent.io/cloud/current/flink/reference/table-api.html#flink-table-api) (Java and Python), and custom [user-defined functions](https://docs.confluent.io/cloud/current/flink/concepts/user-defined-functions.html#flink-sql-udfs).

**Table of Contents**

<!-- toc -->
+ [1.0 Deploying Apache Flink Applications on Confluent Cloud’s Fully Managed Platform](#10-deploying-apache-flink-applications-on-confluent-clouds-fully-managed-platform)
  * [1.1 Avro formatted data](#11-avro-formatted-data)
+ [2.0 Resources](#20-resources)
<!-- tocstop -->


## 1.0 Deploying Apache Flink Applications on Confluent Cloud’s Fully Managed Platform

### 1.1 Avro formatted data
Flink App|Run Script
-|-
**`avro_flight_consolidator_app`**|`../scripts/run-flight-consolidator-ccaf-app-locally.sh --profile=<AWS_SSO_PROFILE_NAME> --catalog-name=<CATALOG_NAME> --database-name=<DATABASE_NAME>`

> Argument placeholder|Replace with
> -|-
> `<AWS_SSO_PROFILE_NAME>`|your AWS SSO profile name for your AWS infrastructue that host your AWS Secrets Manager.
> `<CATALOG_NAME>`|the Environment name of the Kafka Cluster.
> `<DATABASE_NAME>`|the Database name of the Kafka Cluster.

## 2.0 Resources
[Table API on Confluent Cloud for Apache Flink](https://docs.confluent.io/cloud/current/flink/reference/table-api.html#table-api-on-af-long)

[Table API in Confluent Cloud for Apache Flink API Function](https://docs.confluent.io/cloud/current/flink/reference/functions/table-api-functions.html#flink-table-api-functions)

[Information Schema in Confluent Cloud for Apache Flink](https://docs.confluent.io/cloud/current/flink/reference/flink-sql-information-schema.html)
