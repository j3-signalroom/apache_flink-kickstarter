# Java

## DataStream API
[DataStream API](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/datastream_api/)

### Job Examples
[Wade Waldron](https://www.linkedin.com/in/wade-waldron/), Staff Software Practice Lead at [Confluent Inc.](https://www.confluent.io/), author the blog series on [Building Apache Flink Applications in Java](https://developer.confluent.io/courses/flink-java/overview/)

Spoiler Alert

Job|Description
-|-
`DataGeneratorJob`|This job creates fake flight data for fictional airlines **Sunset Air** and **Sky One** Airlines," and sends it to the Kafka topics `sunset` and `skyone` respectively.
`FlightImporterJob`|This job imports flight data from `sunset` and `skyone` Kafka topics and converts it to a unified format for the `flightdata` Kafka topic.
`UserStatisticsJob`|This job processes data from the `flightdata` Kafka topic to aggregate user statistics in the `userstatistics` Kafka topic.

#### Configuration


##### `consumer.properties`
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

##### `producer.properties`
```
bootstrap.servers=<KAFKA CLUSTER URI>
security.protocol=SASL_SSL
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"<KAFKA API KEY>\" password=\"<KAFKA API SECRETS>\";
sasl.mechanism=PLAIN
client.dns.lookup=use_all_dns_ips
acks=all
```

### Run Jobs
```
flink run app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar
```
