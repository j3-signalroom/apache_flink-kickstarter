# Java

## DataStream API
[DataStream API](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/datastream_api/)

### Flink Job (Application) Examples
J3 completed the three main DataStream Job exercises from the blog series on [Building Apache Flink Applications in Java](https://developer.confluent.io/courses/flink-java/overview/):

Job|Description
-|-
`DataGeneratorJob`|This job creates fake flight data for fictional airlines **Sunset Air** and **Sky One** Airlines," and sends it to the Kafka topics `sunset` and `skyone` respectively.
`FlightImporterJob`|This job imports flight data from `sunset` and `skyone` Kafka topics and converts it to a unified format for the `flightdata` Kafka topic.
`UserStatisticsJob`|This job processes data from the `flightdata` Kafka topic to aggregate user statistics in the `userstatistics` Kafka topic.

 Created by [Wade Waldron](https://www.linkedin.com/in/wade-waldron/), Staff Software Practice Lead at [Confluent Inc.](https://www.confluent.io/), and adapted them to showcase two capabilities:

Capability|Description
-|-
Read AWS Secrets Manager and AWS Systems Manager Parameter Store|Instead of relying on the local consumer and producer properties file, the Kafka Cluster API Key, and Kafka Consumer and Kafka Producer client configuration properties are read from the AWS Secrets Manager and AWS Systems Manager Parameter Store.
Custom Source Data Stream|An Apache Flink custom source data stream is a user-defined source of data that is integrated into a Flink application to read and process data from non-standard or custom sources. This custom source can be anything that isn't supported by Flink out of the box, such as proprietary REST APIs, specialized databases, custom hardware interfaces, etc. J3 utilizes a Custom Source Data Stream to read the AWS Secrets Manager secrets and AWS Systems Manager Parameter Store properties during the initial start of a Job, then caches the properties for use by any subsequent events that need these properties.

#### To **`run`** anyone of the Flink Jobs
From your terminal prompt enter:
Job|Run Job reading from local properties files|Run Job reading from AWS
-|-|-
**`DataGeneratorJob`**|`flink run --class apache_flink.kickstarter.datastream_api.DataGeneratorJob app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar`|`flink run --class apache_flink.kickstarter.datastream_api.DataGeneratorJob app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`FlightImporterJob`**|`flink run --class apache_flink.kickstarter.datastream_api.FlightImporterJob app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar`|`flink run --class apache_flink.kickstarter.datastream_api.FlightImporterJob app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`UserStatisticsJob`**|`flink run --class apache_flink.kickstarter.datastream_api.UserStatisticsJob app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar`|`flink run --class apache_flink.kickstarter.datastream_api.UserStatisticsJob app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`

##### Local Consumer and Producer Properties file configuration
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

##### Read from the AWS Secrets Manager and AWS Systems Manager Parameter Store
> **Tip**:  Checkout [iac-confluent_cloud_resources-tf](https://github.com/j3-signalroom/iac-confluent_cloud_resources-tf/blob/main/README.md) Terraform configuration that leverages the IaC Confluent Cloud Resource API Key Rotation Terraform module to create and rotate the API Keys. It then uses AWS Secrets Manager to store the current active API Key for the Schema Registry Cluster and Kafka Cluster. Plus add parameters to the AWS System Manager Parameter Store for a Kafka Consumer and Producer.

**AWS Secrets Manager --- `/confluent_cloud_resource/schema_registry_cluster/java_client`**
> Key|Description
> -|-
> `basic.auth.credentials.source`|Specifies the the format used in the `basic.auth.user.info` property.
> `basic.auth.user.info`|Specifies the API Key and Secret for the Schema Registry Cluster.
> `schema.registry.url`|The HTTP endpoint of the Schema Registry cluster.

**AWS Secrets Manager --- `/confluent_cloud_resource/kafka_cluster/java_client`**
> Key|Description
> -|-
> `sasl.jaas.config`|Java Authentication and Authorization Service (JAAS) for SASL configuration.
> `bootstrap.servers`|The bootstrap endpoint used by Kafka clients to connect to the Kafka cluster.

**AWS Systems Manager Parameter Store --- `/confluent_cloud_resource/consumer_kafka_client`**
> Key|Description
> -|-
> `auto.commit.interval.ms`|The `auto.commit.interval.ms` property in Apache Kafka defines the frequency (in milliseconds) at which the Kafka consumer automatically commits offsets. This is relevant when `enable.auto.commit` is set to true, which allows Kafka to automatically commit the offsets periodically without requiring the application to do so explicitly.
> `auto.offset.reset`|Specifies the behavior of the consumer when there is no committed position (which occurs when the group is first initialized) or when an offset is out of range. You can choose either to reset the position to the `earliest` offset or the `latest` offset (the default).
> `basic.auth.credentials.source`|This property specifies the source of the credentials for basic authentication.
> `client.dns.lookup`|This property specifies how the client should resolve the DNS name of the Kafka brokers.
> `enable.auto.commit`|When set to true, the Kafka consumer automatically commits the offsets of messages it has processed at regular intervals, specified by the `auto.commit.interval.ms` property. If set to false, the application is responsible for committing offsets manually.
> `max.poll.interval.ms`|This property defines the maximum amount of time (in milliseconds) that can pass between consecutive calls to poll() on a consumer. If this interval is exceeded, the consumer will be considered dead, and its partitions will be reassigned to other consumers in the group.
> `request.timeout.ms`|This property sets the maximum amount of time the client will wait for a response from the Kafka broker. If the server does not respond within this time, the client will consider the request as failed and handle it accordingly.
> `sasl.mechanism`|This property specifies the SASL mechanism to be used for authentication.
> `security.protocol`|This property specifies the protocol used to communicate with Kafka brokers.
> `session.timeout.ms`|This property sets the timeout for detecting consumer failures when using Kafka's group management. If the consumer does not send a heartbeat to the broker within this period, it will be considered dead, and its partitions will be reassigned to other consumers in the group.

**AWS Systems Manager Parameter Store --- `/confluent_cloud_resource/producer_kafka_client`**
> Key|Description
> -|-
> `sasl.mechanism`|This property specifies the SASL mechanism to be used for authentication.
> `security.protocol`|This property specifies the protocol used to communicate with Kafka brokers.
> `client.dns.lookup`|This property specifies how the client should resolve the DNS name of the Kafka brokers.
> `acks`|This property specifies the number of acknowledgments the producer requires the leader to have received before considering a request complete.
