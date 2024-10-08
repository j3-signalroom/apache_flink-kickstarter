# Python-based Flink Apps
Examples of Apache Flink® applications showcasing the [DataStream API](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/python/datastream/intro_to_datastream_api/) and [Table API](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/python/table/intro_to_table_api/) in Python using PyFlink [which compiles the apps from Python to Java], featuring AWS, GitHub, Terraform, and Apache Iceberg.  What's the difference between the DataStream API and Table API?  Click [how](../.blog/datastream-vs-table-api.md) to learn the differences.

**Table of Contents**

<!-- toc -->
+ [1.0 Try out these Flink Apps](#10-try-out-these-flink-apps)
+ [2.0 Resources](#20-resources)
<!-- tocstop -->

## 1.0 Try out these Flink Apps
To access the JobManager (`apache_flink-kickstarter-jobmanager-1`) container, open the interactive shell by running:
```bash
docker exec -it -w /opt/flink/python_apps apache_flink-kickstarter-jobmanager-1 /bin/bash
```

This command drops you right into the container, giving you full control to execute commands, explore the file system, or handle any tasks directly.

Finally, to launch one of the Flink applications, choose your app and use the corresponding Flink Run command listed below. Let’s have some fun with Flink!

Flink App|Flink Run Command
-|-
`FlightImporterApp`|`flink run --pyFiles kickstarter/python_files.zip --python kickstarter/flight_importer_app.py --aws_s3_bucket <AWS_S3_BUCKET> --aws_region <AWS_REGION>`
`UserStatisticsApp`|`flink run --pyFiles kickstarter/python_files.zip --python kickstarter/user_statistics_app.py --aws_s3_bucket <AWS_S3_BUCKET> --aws_region <AWS_REGION>`

Argument placeholder|Replace with
-|-
`<AWS_S3_BUCKET>`|specify name of the AWS S3 bucket you chosen during the Terraform creation or created yourself separately.  The AWS S3 bucket is used to store the Apache Iceberg files (i.e., data files, manifest files, manifest list file, and metadata files).
`<AWS_REGION>`|specify the AWS region name where the AWS S3 bucket was created in.

## 2.0 Resources

[Flink Python Docs](https://nightlies.apache.org/flink/flink-docs-master/api/python/)

[PyFlink API Reference](https://nightlies.apache.org/flink/flink-docs-release-1.20/api/python/reference/index.html)

[Apache Flink® Table API on Confluent Cloud - Examples](https://github.com/confluentinc/flink-table-api-python-examples)
