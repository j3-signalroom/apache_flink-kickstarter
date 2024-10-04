# Python-based Flink Apps
Examples of Apache Flink® applications showcasing the [DataStream API](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/python/datastream/intro_to_datastream_api/) and [Table API](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/python/table/intro_to_table_api/) in Python using PyFlink [which compiles the apps from Python to Java], featuring AWS, GitHub, Terraform, and Apache Iceberg.  What's the difference between the DataStream API and Table API?  Click [how](../.blog/datastream-vs-table-api.md) to learn the differences.

**Table of Contents**

<!-- toc -->
+ [1.0 Try out these Flink Apps](#10-try-out-these-flink-apps)
+ [2.0 Resources](#20-resources)
<!-- tocstop -->

## 1.0 Try out these Flink Apps

App|Description
-|-
`FlightImporterApp`|This Python-based app imports flight data from the Kafka topics `airline.sunset` and `airline.skyone`, converts it to a unified format, and then stores the result in the Kafka topic `airline.all`.  Additionally, the app stores the unified flight data in an Apache Iceberg table.
`UserStatisticsApp`|This Python-based app processes data from the `airline.all` Kafka topic to aggregate user statistics in the `airline.user_statistics` Kafka topic.

## 2.0 Resources

[Flink Python Docs](https://nightlies.apache.org/flink/flink-docs-master/api/python/)

[PyFlink API Reference](https://nightlies.apache.org/flink/flink-docs-release-1.20/api/python/reference/index.html)

[Apache Flink® Table API on Confluent Cloud - Examples](https://github.com/confluentinc/flink-table-api-python-examples)
