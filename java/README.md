# Java-based Flink Apps
Discover how Apache Flink® can transform your data pipelines! Explore hands-on examples of Flink applications using the [DataStream API](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/datastream_api/) and [Table API](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/overview/) in Java. You'll see how these technologies integrate seamlessly with AWS, GitHub, Terraform, and Apache Iceberg.

Curious about the differences between the DataStream API and Table API? Click [here](../.blog/datastream-vs-table-api.md) to learn more and find the best fit for your next project.

**Table of Contents**

<!-- toc -->
+ [1.0 Try out these Flink Apps](#10-try-out-these-flink-apps)
<!-- tocstop -->

## 1.0 Try out these Flink Apps
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
**`DataGeneratorApp`**|`flink run --class kickstarter.DataGeneratorApp apache_flink-kickstarter-x.xx.xx.xxx.jar --service-account-user <SERVICE_ACCOUNT_USER>`
**`FlightImporterApp`**|`flink run --class kickstarter.FlightImporterApp apache_flink-kickstarter-x.xx.xx.xxx.jar --service-account-user <SERVICE_ACCOUNT_USER>`
**`FlyerStatssApp`**|`flink run --class kickstarter.FlyerStatsApp apache_flink-kickstarter-x.xx.xx.xxx.jar --service-account-user <SERVICE_ACCOUNT_USER>`

> Argument placeholder|Replace with
> -|-
> `<SERVICE_ACCOUNT_USER>`|specify the name of the service account user, used in the the AWS Secrets and Parameter Store Path name.