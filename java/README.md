# Java
Examples of Flink Apps written in Java.

## DataStream API
[DataStream API](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/datastream_api/)

### Flink App (Application) Examples
J3 completed the three main DataStream app exercises from the blog series on [Building Apache Flink Applications in Java](https://developer.confluent.io/courses/flink-java/overview/):

App|Description
-|-
`DataGeneratorApp`|This app creates fake flight data for fictional airlines **Sunset Air** and **Sky One** Airlines," and sends it to the Kafka topics `airline.sunset` and `airline.skyone` respectively.
`FlightImporterApp`|This app imports flight data from `airline.sunset` and `airline.skyone` Kafka topics and converts it to a unified format for the `airline.all` Kafka topic.
`UserStatisticsApp`|This app processes data from the `airline.all` Kafka topic to aggregate user statistics in the `airline.user_statistics` Kafka topic.

 Originally created by [Wade Waldron](https://www.linkedin.com/in/wade-waldron/), Staff Software Practice Lead at [Confluent Inc.](https://www.confluent.io/), and adapted them to showcase three capabilities:

No.|Capability|Description
-|-|-
1.|Read AWS Secrets Manager and AWS Systems Manager Parameter Store|Instead of relying on the local consumer and producer properties file, the Kafka Cluster API Key, and Kafka Consumer and Kafka Producer client configuration properties are read from the AWS Secrets Manager and AWS Systems Manager Parameter Store.
2.|Custom Source Data Stream|An Apache Flink custom source data stream is a user-defined source of data that is integrated into a Flink application to read and process data from non-standard or custom sources. This custom source can be anything that isn't supported by Flink out of the box, such as proprietary REST APIs, specialized databases, custom hardware interfaces, etc. J3 utilizes a Custom Source Data Stream to read the AWS Secrets Manager secrets and AWS Systems Manager Parameter Store properties during the initial start of a App, then caches the properties for use by any subsequent events that need these properties.
3.|Sinking to Apache Iceberg **(COMING SOON)**|The combination of Apache Flink and Apache Iceberg provides several advantages. Iceberg’s capabilities, including snapshot isolation for reads and writes, the ability to handle multiple concurrent operations, ACID-compliant queries, and incremental reads, enable Flink to perform operations that were traditionally challenging with older table formats. Together, they offer an efficient and scalable platform for processing large-scale data, especially for streaming use cases.

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

Finally, to run any of the Flink Apps, enter any one of the commands on the command-line below:

App|Run App reading from local properties files|Run App reading from AWS
-|-|-
**`DataGeneratorApp`**|`flink run --class apache_flink.kickstarter.datastream_api.DataGeneratorApp app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar`|`flink run --class apache_flink.kickstarter.datastream_api.DataGeneratorApp app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`FlightImporterApp`**|`flink run --class apache_flink.kickstarter.datastream_api.FlightImporterApp app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar`|`flink run --class apache_flink.kickstarter.datastream_api.FlightImporterApp app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`UserStatisticsApp`**|`flink run --class apache_flink.kickstarter.datastream_api.UserStatisticsApp app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar`|`flink run --class apache_flink.kickstarter.datastream_api.UserStatisticsApp app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`

##### Local Consumer and Producer Properties file configuration (if not using AWS Secrets Manager and AWS Systems Manager Parameter Store)
**`consumer.properties`**
```
bootstrap.servers=<KAFKA CLUSTER URI>
security.protocol=SASL_SSL
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"<KAFKA API KEY>\" password=\"<KAFKA API SECRETS>\";
sasl.mechanism=PLAIN
group.id=my-consumer-group
auto.offset.reset=earliest
enable.auto.commit=true
auto.commit.interval.ms=1000
fetch.max.wait.ms=500
fetch.min.bytes=1
max.poll.records=500
max.partition.fetch.bytes=1048576
client.id=my-consumer-client
value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
request.timeout.ms=30000
max.poll.interval.ms=300000
```

**`producer.properties`**
```
bootstrap.servers=<KAFKA CLUSTER URI>
security.protocol=SASL_SSL
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"<KAFKA API KEY>\" password=\"<KAFKA API SECRETS>\";
sasl.mechanism=PLAIN
client.dns.lookup=use_all_dns_ips
acks=all
```
