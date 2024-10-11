# Python-based Flink Apps
Discover how Apache Flink® can transform your data pipelines! Explore hands-on examples of Flink applications using the [DataStream API](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/python/datastream/intro_to_datastream_api/), [Table API](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/python/table/intro_to_table_api/), and [Flink SQL](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/table/sql/overview/#sql)—all built in Python with PyFlink, which compiles these apps to Java. You'll see how these technologies integrate seamlessly with AWS, GitHub, Terraform, and Apache Iceberg.

Curious about the differences between the DataStream API and Table API? Click [here](../.blog/datastream-vs-table-api.md) to learn more and find the best fit for your next project.

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
**`FlightImporterApp`**|`flink run --pyFiles kickstarter/python_files.zip --python kickstarter/flight_importer_app.py --aws_s3_bucket <AWS_S3_BUCKET>`
**`FlyerStatsApp`**|`flink run --pyFiles kickstarter/python_files.zip --python kickstarter/flyer_stats_app.py --aws_s3_bucket <AWS_S3_BUCKET>`

> Argument placeholder|Replace with
> -|-
> `<AWS_S3_BUCKET>`|specify name of the AWS S3 bucket you chosen during the Terraform creation or created yourself separately.  The AWS S3 bucket is used to store the Apache Iceberg files (i.e., data files, manifest files, manifest list file, and metadata files).

## 2.0 Resources

[Flink Python Docs](https://nightlies.apache.org/flink/flink-docs-master/api/python/)

[PyFlink API Reference](https://nightlies.apache.org/flink/flink-docs-release-1.20/api/python/reference/index.html)

[Apache Flink® Table API on Confluent Cloud - Examples](https://github.com/confluentinc/flink-table-api-python-examples)
