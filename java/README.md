# Writing Flink Apps in Java
Examples of Apache Flink® applications showcasing the [DataStream API](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/datastream_api/) and [Table API](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/overview/) in Java, featuring AWS, GitHub, Terraform, and Apache Iceberg.  What's the difference between the DataStream API and Table API?  Click to learn [how](../.blog/datastream-vs-table-api.md). 

## Try out these Flink Apps
Open the repo from the `Java` subfolder.  Then run:

> *This command ensures a pristine build environment.  By removing previous build artifacts, this command guarantees that developers initiate their projects from a clean slate, minimizing inconsistencies and fostering a more reliable build process.*

```
./gradlew app:clean
```

Now build JAR file that contains all the Flink Apps on it, by running:

```
./gradlew app:build
```

Logon to the `apache_flink-kickstarter-jobmanager-1` container's Interative Shell:
> *This allows you to interact with the container as if you were inside its terminal, enabling you to run commands, inspect the file system, or perform other tasks interactively within the container.*
```
docker exec -it -w /opt/flink/apps apache_flink-kickstarter-jobmanager-1 /bin/bash
```

Finally, to run any of the Flink Apps, choose the app and then enter the corresponding CLI command:

App|Commands for CLI
-|-
**`DataGeneratorApp`**|`flink run --class apache_flink.kickstarter.DataGeneratorApp apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`FlightImporterApp`**|`flink run --class apache_flink.kickstarter.FlightImporterApp apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`UserStatisticsApp`**|`flink run --class apache_flink.kickstarter.UserStatisticsApp apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
