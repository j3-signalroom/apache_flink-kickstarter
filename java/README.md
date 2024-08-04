# Java Examples

## Jobs

Job Examples|Description
-|-
`HellowWorldJob`|
`DataGeneratorJob`|
`SimpleKafkaSinkJob`|
`FlightImporterJob`|
`UserStatisticsJob`|

### Configuration


#### `consumer.properties`
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

#### `producer.properties`
```
bootstrap.servers=<KAFKA CLUSTER URI>
security.protocol=SASL_SSL
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"<KAFKA API KEY>\" password=\"<KAFKA API SECRETS>\";
sasl.mechanism=PLAIN
client.dns.lookup=use_all_dns_ips
acks=all
```

## Run Jobs
```
$ flink run app/build/libs/apache_flink-kickstarter-x.xx.xx.xxx.jar
```
