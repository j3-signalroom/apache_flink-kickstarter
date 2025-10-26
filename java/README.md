# Flink Applications Powered by Java on a locally running Apache Flink Cluster in Docker
Discover how Apache Flink® can transform your data pipelines! Explore hands-on examples of Flink applications using the [DataStream API](https://nightlies.apache.org/flink/flink-docs-release-2.1/docs/dev/datastream/overview/) and [Table API](https://nightlies.apache.org/flink/flink-docs-release-2.1/docs/dev/table/overview/) in Java. You'll see how these technologies integrate seamlessly with AWS Services, Apache Kafka, and  Apache Iceberg.

Curious about the differences between the DataStream API and Table API? Click [here](../.blog/datastream-vs-table-api.md) to learn more and find the best fit for your next project.

**Table of Contents**

<!-- toc -->
+ [1.0 Power up the Apache Flink Docker containers](#10-power-up-the-apache-flink-docker-containers)
+ [2.0 Discover What You Can Do with These Flink Apps](#20-discover-what-you-can-do-with-these-flink-apps)
    - [2.1 Avro formatted data](#21-avro-formatted-data)
        + [2.1.1 Avro Java Classes Special Consideration](#211-avro-java-classes-special-consideration)
    - [2.2 JSON formatted data](#22-json-formatted-data)
<!-- tocstop -->

## 1.0 Power up the Apache Flink Docker containers

> **Prerequisite**
> 
> Before you can run `./deploy-flink.sh` Bash script, you need to install the [`aws2-wrap`](https://pypi.org/project/aws2-wrap/#description) utility.  If you have a Mac machine, run this command from your Terminal:
> ````bash
> brew install aws2-wrap
> ````
>
> If you do not, make sure you have Python3.x installed on your machine, and run this command from your Terminal:
> ```bash
> pip install aws2-wrap
> ```

This section guides you through the local setup (on one machine but in separate containers) of the Apache Flink cluster in Session mode using Docker containers with support for Apache Iceberg.  Run the `bash` script below to start the Apache Flink cluster in Session Mode on your machine:

```bash
./deploy-flink.sh <on | off> --profile=<AWS_SSO_PROFILE_NAME>
                             --chip=<amd64 | arm64>
                             --flink-language=java
```
> Argument placeholder|Replace with
> -|-
> `<DOCKER_SWITCH>`|`on` to start up your very own local Apache Cluster running in Docker containers, otherwise `off` to stop the Docker containers.
> `<AWS_SSO_PROFILE_NAME>`|your AWS SSO profile name for your AWS infrastructue that host your AWS Secrets Manager.
> `<CHIP>`|if you are running on a Mac with M1, M2, or M3 chip, use `arm64`.  Otherwise, use `amd64`.

To learn more about this script, click [here](../.blog/deploy-flink-script-explanation.md).

## 2.0 Discover What You Can Do with These Flink Apps
To access the Flink JobManager (`apache_flink-kickstarter-jobmanager-1`) container, open the interactive shell by running:
```bash
docker exec -it -w /opt/flink/java_apps/app/build/libs apache_flink-kickstarter-jobmanager-1 /bin/bash
```

This command drops you right into the container, giving you full control to execute commands, explore the file system, or handle any tasks directly.

Finally, to launch one of the **pre-complied** Flink applications, choose your app and use the corresponding [`flink run`](https://nightlies.apache.org/flink/flink-docs-master/docs/deployment/cli/) command listed below. Let’s have some fun with Flink!

### 2.1 Avro formatted data
Flink App|Flink Run Command
-|-
**`AvroDataGeneratorApp`**|`flink run --class kickstarter.AvroDataGeneratorApp apache_flink-kickstarter-dev-SNAPSHOT.jar`
**`AvroFlightConsolidatorApp`**|`flink run --class kickstarter.AvroFlightConsolidatorApp apache_flink-kickstarter-dev-SNAPSHOT.jar`
**`AvroFlyerStatsApp`**|`flink run --class kickstarter.AvroFlyerStatsApp apache_flink-kickstarter-dev-SNAPSHOT.jar`

#### 2.1.1 Avro Java Classes Special Consideration
Whenever any of the Flink Apps [`Avro models`](app/src/main/java/kickstarter/model/avro/) need to be updated, the [`avro-tools-1.12.0.jar`](https://avro.apache.org/docs/++version++/getting-started-java/#serializing-and-deserializing-with-code-generation) must be used afterwards to generate the respective Java class. This is necessary to ensure that the Avro schema is in sync with the Java class. To generate the Java class, run the following command from the [`apache_flink-kickstarter-jobmanager-1`](#20-discover-what-you-can-do-with-these-flink-apps) Docker container:

> To download the `avro-tools-1.12.0.jar`, click [here](https://repo1.maven.org/maven2/org/apache/avro/avro-tools/1.12.0/avro-tools-1.12.0.jar).

```bash 
java -jar /path/to/avro-tools-1.12.0.jar compile -string schema app/src/main/java/kickstarter/model/avro/AirlineAvroData.avsc app/src/main/java/

java -jar /path/to/avro-tools-1.12.0.jar compile -string schema app/src/main/java/kickstarter/model/avro/FlightAvroData.avsc app/src/main/java/

java -jar /path/to/avro-tools-1.12.0.jar compile -string schema app/src/main/java/kickstarter/model/avro/FlyerStatsAvroData.avsc app/src/main/java/
```

> Note: Make sure to replace `/path/to/avro-tools-1.12.0.jar` with the actual path where you downloaded the `avro-tools-1.12.0.jar` file inside the Docker container.

### 2.2 JSON formatted data
Flink App|Flink Run Command
-|-
**`JsonDataGeneratorApp`**|`flink run --class kickstarter.JsonDataGeneratorApp apache_flink-kickstarter-dev-SNAPSHOT.jar`
**`JsonFlightConsolidatorApp`**|`flink run --class kickstarter.JsonFlightConsolidatorApp apache_flink-kickstarter-dev-SNAPSHOT.jar`
**`JsonFlyerStatsApp`**|`flink run --class kickstarter.JsonFlyerStatsApp apache_flink-kickstarter-dev-SNAPSHOT.jar`
