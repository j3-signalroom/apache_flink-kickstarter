# Java
Examples of Flink Apps written in Java.

## DataStream API
[DataStream API](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/datastream_api/)

#### To **`run`** anyone of the Flink Apps
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
**`DataGeneratorApp`**|`flink run --class apache_flink.kickstarter.datastream_api.DataGeneratorApp apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`FlightImporterApp`**|`flink run --class apache_flink.kickstarter.datastream_api.FlightImporterApp apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`UserStatisticsApp`**|`flink run --class apache_flink.kickstarter.datastream_api.UserStatisticsApp apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
