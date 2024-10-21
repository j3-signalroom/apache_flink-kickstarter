# Flink Applications Powered by Java
Discover how Apache Flink® can transform your data pipelines! Explore hands-on examples of Flink applications using the [DataStream API](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/datastream_api/) and [Table API](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/overview/) in Java. You'll see how these technologies integrate seamlessly with AWS Services, Apache Kafka, and  Apache Iceberg.

Curious about the differences between the DataStream API and Table API? Click [here](../.blog/datastream-vs-table-api.md) to learn more and find the best fit for your next project.

**Table of Contents**

<!-- toc -->
+ [1.0 Power up the Apache Flink Docker containers](#10-power-up-the-apache-flink-docker-containers)
+ [2.0 Discover What You Can Do with These Flink Apps](#20-discover-what-you-can-do-with-these-flink-apps)
<!-- tocstop -->

## 1.0 Power up the Apache Flink Docker containers

> **Prerequisite**
> 
> Before you can run `scripts/run-flink-locally.sh` Bash script, you need to install the [`aws2-wrap`](https://pypi.org/project/aws2-wrap/#description) utility.  If you have a Mac machine, run this command from your Terminal:
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
scripts/run-flink-locally.sh on --profile=<AWS_SSO_PROFILE_NAME>
                                --chip=<amd64 | arm64>
                                --flink-language=java
                                [--aws-s3-bucket=<AWS_S3_BUCKET_NAME>]
```
> Argument placeholder|Replace with
> -|-
> `<ACTIVATE_DOCKER_CONTAINER>`|`on` to turn on Flink locally, otherwise `off` to turn Flink off.
> `<AWS_SSO_PROFILE_NAME>`|your AWS SSO profile name for your AWS infrastructue that host your AWS Secrets Manager.
> `<CHIP>`|if you are running on a Mac with M1, M2, or M3 chip, use `arm64`.  Otherwise, use `amd64`.
> `<FLINK_LANGUAGE>`|`java` to specify Java is the language base of the Flink Apps you plan on running.  Otherwise, specifiy `python` if the language base of the Flink Apps are Python.
> `<AWS_S3_BUCKET_NAME>`|**[Optional]** can specify the name of the AWS S3 bucket used to store Apache Iceberg files.

To learn more about this script, click [here](../.blog/run-flink-locally-script-explanation.md).

## 2.0 Discover What You Can Do with These Flink Apps
Before diving in, I want to point out a choice I made for the project: I used Gradle instead of Maven for build automation. Why ![gradle](../.blog/images/gradle-logo.png)[Gradle](https://gradle.com/)? Well, it's not just more straightforward—it's also more flexible and powerful. I opted for the Kotlin-supported version of Gradle, which brings some major perks: type safety, seamless IDE support, and the benefits of a modern programming language, unlike Maven's XML-heavy approach. Plus, Kotlin's features help speed up our builds, which is always a nice bonus.

Alright, let’s get hands-on! First, navigate to the project located in the Java subfolder, and let’s start with a clean slate. Run:

```bash
./gradlew app:clean
```

This command wipes out any previous build artifacts, ensuring we start with a pristine environment. It helps us avoid inconsistencies and makes the build process more reliable for everyone involved.

Next, let’s build the JAR file containing all the Flink applications:

```bash
./gradlew app:build
```

Now, to access the JobManager (`apache_flink-kickstarter-jobmanager-1`) container, open the interactive shell by running:
```bash
docker exec -it -w /opt/flink/java_apps apache_flink-kickstarter-jobmanager-1 /bin/bash
```

This command drops you right into the container, giving you full control to execute commands, explore the file system, or handle any tasks directly.

Finally, to launch one of the Flink applications, choose your app and use the corresponding Flink Run command listed below. Let’s have some fun with Flink!

Flink App|Flink Run Command
-|-
**`DataGeneratorApp`**|`flink run --class kickstarter.DataGeneratorApp apache_flink-kickstarter-x.xx.xx.xxx.jar --service-account-user <SERVICE_ACCOUNT_USER> --aws-region <AWS_REGION_NAME>`
**`FlightImporterApp`**|`flink run --class kickstarter.FlightImporterApp apache_flink-kickstarter-x.xx.xx.xxx.jar --service-account-user <SERVICE_ACCOUNT_USER>`
**`FlyerStatsApp`**|`flink run --class kickstarter.FlyerStatsApp apache_flink-kickstarter-x.xx.xx.xxx.jar --service-account-user <SERVICE_ACCOUNT_USER>`

> Argument placeholder|Replace with
> -|-
> `<SERVICE_ACCOUNT_USER>`|specify the name of the service account user, used in the the AWS Secrets and Parameter Store Path name.
> `<AWS_REGION_NAME>`|specify the AWS Region your AWS Glue infrastructure resides.