resource "aws_ssm_parameter" "consumer_kafka_client_auto_commit_interval_ms" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/auto.commit.interval.ms"
  description = "The 'auto.commit.interval.ms' property in Apache Kafka defines the frequency (in milliseconds) at which the Kafka consumer automatically commits offsets. This is relevant when 'enable.auto.commit' is set to true, which allows Kafka to automatically commit the offsets periodically without requiring the application to do so explicitly."
  type        = "String"
  value       = "1000"
}

resource "aws_ssm_parameter" "consumer_kafka_client_auto_offset_reset" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/auto.offset.reset"
  description = "Specifies the behavior of the consumer when there is no committed position (which occurs when the group is first initialized) or when an offset is out of range. You can choose either to reset the position to the 'earliest' offset or the 'latest' offset (the default)."
  type        = "String"
  value       = "${var.auto_offset_reset}"
}

resource "aws_ssm_parameter" "consumer_kafka_client_basic_auth_credentials_source" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/basic.auth.credentials.source"
  description = "This property specifies the source of the credentials for basic authentication."
  type        = "String"
  value       = "USER_INFO"
}

resource "aws_ssm_parameter" "consumer_kafka_client_client_dns_lookup" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/client.dns.lookup"
  description = "This property specifies how the client should resolve the DNS name of the Kafka brokers."
  type        = "String"
  value       = "use_all_dns_ips"
}

resource "aws_ssm_parameter" "consumer_kafka_client_enable_auto_commit" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/enable.auto.commit"
  description = "When set to true, the Kafka consumer automatically commits the offsets of messages it has processed at regular intervals, specified by the 'auto.commit.interval.ms' property. If set to false, the application is responsible for committing offsets manually."
  type        = "String"
  value       = "true"
}

resource "aws_ssm_parameter" "consumer_kafka_client_max_poll_interval_ms" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/max.poll.interval.ms"
  description = "This property defines the maximum amount of time (in milliseconds) that can pass between consecutive calls to poll() on a consumer. If this interval is exceeded, the consumer will be considered dead, and its partitions will be reassigned to other consumers in the group."
  type        = "String"
  value       = "300000"
}

resource "aws_ssm_parameter" "consumer_kafka_client_request_timeout_ms" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/request.timeout.ms"
  description = "This property sets the maximum amount of time the client will wait for a response from the Kafka broker. If the server does not respond within this time, the client will consider the request as failed and handle it accordingly."
  type        = "String"
  value       = "60000"
}

resource "aws_ssm_parameter" "consumer_kafka_client_sasl_mechanism" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/sasl.mechanism"
  description = "This property specifies the SASL mechanism to be used for authentication."
  type        = "String"
  value       = "PLAIN"
}

resource "aws_ssm_parameter" "consumer_kafka_client_security_protocol" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/security.protocol"
  description = "This property specifies the protocol used to communicate with Kafka brokers."
  type        = "String"
  value       = "SASL_SSL"
}

resource "aws_ssm_parameter" "consumer_kafka_client_session_timeout_ms" {
  name        = "${local.confluent_secrets_path_prefix}/consumer_kafka_client/session.timeout.ms"
  description = "This property sets the timeout for detecting consumer failures when using Kafka's group management. If the consumer does not send a heartbeat to the broker within this period, it will be considered dead, and its partitions will be reassigned to other consumers in the group."
  type        = "String"
  value       = "90000"
}

resource "aws_ssm_parameter" "producer_kafka_client_sasl_mechanism" {
  name        = "${local.confluent_secrets_path_prefix}/producer_kafka_client/sasl.mechanism"
  description = "This property specifies the SASL mechanism to be used for authentication."
  type        = "String"
  value       = "PLAIN"
}

resource "aws_ssm_parameter" "producer_kafka_client_security_protocol" {
  name        = "${local.confluent_secrets_path_prefix}/producer_kafka_client/security.protocol"
  description = "This property specifies the protocol used to communicate with Kafka brokers."
  type        = "String"
  value       = "SASL_SSL"
}

resource "aws_ssm_parameter" "producer_kafka_client_client_dns_lookup" {
  name        = "${local.confluent_secrets_path_prefix}/producer_kafka_client/client.dns.lookup"
  description = "This property specifies how the client should resolve the DNS name of the Kafka brokers."
  type        = "String"
  value       = "use_all_dns_ips"

}

resource "aws_ssm_parameter" "producer_kafka_client_acks" {
  name        = "${local.confluent_secrets_path_prefix}/producer_kafka_client/acks"
  description = "This property specifies the number of acknowledgments the producer requires the leader to have received before considering a request complete."
  type        = "String"
  value       = "all"
}

resource "aws_ssm_parameter" "producer_kafka_client_transaction_timeout_ms" {
  name        = "${local.confluent_secrets_path_prefix}/producer_kafka_client/transaction.timeout.ms"
  description = "This property specifies the maximum amount of time (in milliseconds) a transaction can remain open. This property is critical for Kafka producers that use transactional messaging to ensure exactly-once delivery semantics."
  type        = "String"
  value       = "60000"
}

resource "aws_ssm_parameter" "producer_kafka_client_compression_type" {
  name        = "${local.confluent_secrets_path_prefix}/producer_kafka_client/compression.type"
  description = "This property specifies the compression type for all data generated by the producer.  The valid values are 'none', 'gzip', 'snappy', 'lz4', and 'zstd'."
  type        = "String"
  value       = "gzip"
}

resource "aws_ssm_parameter" "producer_kafka_client_auto_create_topics_enable" {
  name        = "${local.confluent_secrets_path_prefix}/producer_kafka_client/auto.create.topics.enable"
  description = "This property is a broker-level configuration in Apache Kafka that determines whether the Kafka broker should automatically create a topic when a producer or consumer attempts to access a non-existent topic."
  type        = "String"
  value       = "true"
}
