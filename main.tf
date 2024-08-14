terraform {
    cloud {
      organization ="<TERRAFORM CLOUD ORGANIZATION NAME>"

        workspaces {
            name = "<TERRAFORM CLOUD ORGANIZATION's WORKSPACE NAME>"
        }
  }

  required_providers {
        confluent = {
            source  = "confluentinc/confluent"
            version = "~> 1.82.0"
        }
        aws = {
            source  = "hashicorp/aws"
            version = "~> 5.60.0"
        }
    }
}

provider "confluent" {
  cloud_api_key    = var.confluent_cloud_api_key
  cloud_api_secret = var.confluent_cloud_api_secret
}

provider "aws" {
    region  = var.aws_region
}

locals {
  cloud          = "AWS"
  secrets_prefix = "/confluent_cloud_resource"
}

# Reference the Confluent Cloud
data "confluent_organization" "env" {}

# Create the Confluent Cloud Environment
resource "confluent_environment" "env" {
    display_name = "${var.aws_profile}"
}

# Create the Service Account for the Kafka Cluster API
resource "confluent_service_account" "schema_registry_cluster_api" {
    display_name = "${var.aws_profile}-environment-api"
    description  = "Environment API Service Account"
}

# Config the environment's schema registry
data "confluent_schema_registry_region" "env" {
    cloud   = local.cloud
    region  = var.aws_region
    package = "ESSENTIALS"
}

resource "confluent_schema_registry_cluster" "env" {
  package = data.confluent_schema_registry_region.env.package

  environment {
    id = confluent_environment.env.id
  }

  region {
    # See https://docs.confluent.io/cloud/current/stream-governance/packages.html#stream-governance-regions
    # Schema Registry and Kafka clusters can be in different regions as well as different cloud providers,
    # but you should to place both in the same cloud and region to restrict the fault isolation boundary.
    # A fault isolation boundary, also known as a swimlane, is a concept that separates services into failure 
    # domains to limit the impact of a failure to a specific number of components.
    id = data.confluent_schema_registry_region.env.id
  }
}

# Create the Environment API Key Pairs, rotate them in accordance to a time schedule, and provide the current
# acitve API Key Pair to use
module "schema_registry_cluster_api_key_rotation" {
    
    source  = "github.com/j3-signalroom/iac-confluent_cloud_resource_api_key_rotation-tf_module"

    # Required Input(s)
    owner = {
        id          = confluent_service_account.schema_registry_cluster_api.id
        api_version = confluent_service_account.schema_registry_cluster_api.api_version
        kind        = confluent_service_account.schema_registry_cluster_api.kind
    }

    resource = {
        id          = confluent_schema_registry_cluster.env.id
        api_version = confluent_schema_registry_cluster.env.api_version
        kind        = confluent_schema_registry_cluster.env.kind

        environment = {
            id = confluent_environment.env.id
        }
    }

    confluent_cloud_api_key    = var.confluent_cloud_api_key
    confluent_cloud_api_secret = var.confluent_cloud_api_secret

    # Optional Input(s)
    key_display_name = "Confluent Schema Registry Cluster Service Account API Key - {date} - Managed by Terraform Cloud"
    number_of_api_keys_to_retain = var.number_of_api_keys_to_retain
    day_count = var.day_count
}

# Create the Kafka cluster
resource "confluent_kafka_cluster" "kafka_cluster" {
    display_name = "kafka_cluster"
    availability = "SINGLE_ZONE"
    cloud        = local.cloud
    region       = var.aws_region
    basic {}

    environment {
        id = confluent_environment.env.id
    }
}

# Create the Service Account for the Kafka Cluster API
resource "confluent_service_account" "kafka_cluster_api" {
    display_name = "${var.aws_profile}-kafka_cluster-api"
    description  = "Kafka Cluster API Service Account"
}

# Create the Kafka Cluster API Key Pairs, rotate them in accordance to a time schedule, and provide the current acitve API Key Pair
# to use
module "kafka_cluster_api_key_rotation" {
    source  = "github.com/j3-signalroom/iac-confluent_cloud_resource_api_key_rotation-tf_module"

    #Required Input(s)
    owner = {
        id          = confluent_service_account.kafka_cluster_api.id
        api_version = confluent_service_account.kafka_cluster_api.api_version
        kind        = confluent_service_account.kafka_cluster_api.kind
    }

    resource = {
        id          = confluent_kafka_cluster.kafka_cluster.id
        api_version = confluent_kafka_cluster.kafka_cluster.api_version
        kind        = confluent_kafka_cluster.kafka_cluster.kind

        environment = {
            id = confluent_environment.env.id
        }
    }

    confluent_cloud_api_key    = var.confluent_cloud_api_key
    confluent_cloud_api_secret = var.confluent_cloud_api_secret

    # Optional Input(s)
    key_display_name = "Confluent Kafka Cluster Service Account API Key - {date} - Managed by Terraform Cloud"
    number_of_api_keys_to_retain = var.number_of_api_keys_to_retain
    day_count = var.day_count
}

# Create the Schema Registry Cluster Secrets: API Key Pair and REST endpoint
resource "aws_secretsmanager_secret" "schema_registry_cluster_api_key" {
    name = "${local.secrets_prefix}/schema_registry_cluster/java_client"
    description = "Schema Registry Cluster secrets"
}

resource "aws_secretsmanager_secret_version" "schema_registry_cluster_api_key" {
    secret_id     = aws_secretsmanager_secret.schema_registry_cluster_api_key.id
    secret_string = jsonencode({"basic.auth.credentials.source": "USER_INFO",
                                "basic.auth.user.info": "${module.schema_registry_cluster_api_key_rotation.active_api_key.id}:${module.schema_registry_cluster_api_key_rotation.active_api_key.secret}",
                                "schema.registry.url": "${confluent_schema_registry_cluster.env.rest_endpoint}"})
}

# Create the Kafka Cluster Secrets: API Key Pair, JAAS (Java Authentication and Authorization) representation,
# bootstrap server URI and REST endpoint
resource "aws_secretsmanager_secret" "kafka_cluster_api_key" {
    name = "${local.secrets_prefix}/kafka_cluster/java_client"
    description = "Kafka Cluster secrets"
}

resource "aws_secretsmanager_secret_version" "kafka_cluster_api_key" {
    secret_id     = aws_secretsmanager_secret.kafka_cluster_api_key.id
    secret_string = jsonencode({"sasl.jaas.config": "org.apache.kafka.common.security.plain.PlainLoginModule required username='${module.kafka_cluster_api_key_rotation.active_api_key.id}' password='${module.kafka_cluster_api_key_rotation.active_api_key.secret}';",
                                "bootstrap.servers": replace(confluent_kafka_cluster.kafka_cluster.bootstrap_endpoint, "SASL_SSL://", "")})
}

resource "aws_ssm_parameter" "consumer_kafka_client_auto_commit_interval_ms" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/auto.commit.interval.ms"
  description = "The 'auto.commit.interval.ms' property in Apache Kafka defines the frequency (in milliseconds) at which the Kafka consumer automatically commits offsets. This is relevant when 'enable.auto.commit' is set to true, which allows Kafka to automatically commit the offsets periodically without requiring the application to do so explicitly."
  type        = "String"
  value       = "1000"
}

resource "aws_ssm_parameter" "consumer_kafka_client_auto_offset_reset" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/auto.offset.reset"
  description = "Specifies the behavior of the consumer when there is no committed position (which occurs when the group is first initialized) or when an offset is out of range. You can choose either to reset the position to the 'earliest' offset or the 'latest' offset (the default)."
  type        = "String"
  value       = "${var.auto_offset_reset}"
}

resource "aws_ssm_parameter" "consumer_kafka_client_basic_auth_credentials_source" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/basic.auth.credentials.source"
  description = "This property specifies the source of the credentials for basic authentication."
  type        = "String"
  value       = "USER_INFO"
}

resource "aws_ssm_parameter" "consumer_kafka_client_client_dns_lookup" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/client.dns.lookup"
  description = "This property specifies how the client should resolve the DNS name of the Kafka brokers."
  type        = "String"
  value       = "use_all_dns_ips"
}

resource "aws_ssm_parameter" "consumer_kafka_client_enable_auto_commit" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/enable.auto.commit"
  description = "When set to true, the Kafka consumer automatically commits the offsets of messages it has processed at regular intervals, specified by the 'auto.commit.interval.ms' property. If set to false, the application is responsible for committing offsets manually."
  type        = "String"
  value       = "true"
}

resource "aws_ssm_parameter" "consumer_kafka_client_max_poll_interval_ms" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/max.poll.interval.ms"
  description = "This property defines the maximum amount of time (in milliseconds) that can pass between consecutive calls to poll() on a consumer. If this interval is exceeded, the consumer will be considered dead, and its partitions will be reassigned to other consumers in the group."
  type        = "String"
  value       = "300000"
}

resource "aws_ssm_parameter" "consumer_kafka_client_request_timeout_ms" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/request.timeout.ms"
  description = "This property sets the maximum amount of time the client will wait for a response from the Kafka broker. If the server does not respond within this time, the client will consider the request as failed and handle it accordingly."
  type        = "String"
  value       = "60000"
}

resource "aws_ssm_parameter" "consumer_kafka_client_sasl_mechanism" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/sasl.mechanism"
  description = "This property specifies the SASL mechanism to be used for authentication."
  type        = "String"
  value       = "PLAIN"
}

resource "aws_ssm_parameter" "consumer_kafka_client_security_protocol" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/security.protocol"
  description = "This property specifies the protocol used to communicate with Kafka brokers."
  type        = "String"
  value       = "SASL_SSL"
}

resource "aws_ssm_parameter" "consumer_kafka_client_session_timeout_ms" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/session.timeout.ms"
  description = "This property sets the timeout for detecting consumer failures when using Kafka's group management. If the consumer does not send a heartbeat to the broker within this period, it will be considered dead, and its partitions will be reassigned to other consumers in the group."
  type        = "String"
  value       = "90000"
}

resource "aws_ssm_parameter" "consumer_kafka_client_group_id" {
  name        = "/confluent_cloud_resource/consumer_kafka_client/group.id"
  description = "This property sets what group a consumer belongs to."
  type        = "String"
  value       = "apache-flink-kickstarter-consumer"
}

resource "aws_ssm_parameter" "producer_kafka_client_sasl_mechanism" {
  name        = "/confluent_cloud_resource/producer_kafka_client/sasl.mechanism"
  description = "This property specifies the SASL mechanism to be used for authentication."
  type        = "String"
  value       = "PLAIN"
}

resource "aws_ssm_parameter" "producer_kafka_client_security_protocol" {
  name        = "/confluent_cloud_resource/producer_kafka_client/security.protocol"
  description = "This property specifies the protocol used to communicate with Kafka brokers."
  type        = "String"
  value       = "SASL_SSL"
}

resource "aws_ssm_parameter" "producer_kafka_client_client_dns_lookup" {
  name        = "/confluent_cloud_resource/producer_kafka_client/client.dns.lookup"
  description = "This property specifies how the client should resolve the DNS name of the Kafka brokers."
  type        = "String"
  value       = "use_all_dns_ips"

}

resource "aws_ssm_parameter" "producer_kafka_client_acks" {
  name        = "/confluent_cloud_resource/producer_kafka_client/acks"
  description = "This property specifies the number of acknowledgments the producer requires the leader to have received before considering a request complete."
  type        = "String"
  value       = "all"
}

resource "confluent_kafka_topic" "airline_skyone" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "airline.skyone"
  partitions_count   = 1
  rest_endpoint      = confluent_kafka_cluster.kafka_cluster.rest_endpoint

  config = {
    "retention.bytes" = "-1"
    "retention.ms"    = "-1"
  }
  credentials {
    key    = module.kafka_cluster_api_key_rotation.active_api_key.id
    secret = module.kafka_cluster_api_key_rotation.active_api_key.secret
  }
}

resource "confluent_role_binding" "topic-airline_skyone-write" {
  principal   = "User:${confluent_service_account.kafka_cluster_api.id}"
  role_name   = "DeveloperWrite"
  crn_pattern = "${confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${confluent_kafka_cluster.kafka_cluster.id}/topic=${confluent_kafka_topic.airline_skyone.topic_name}"
}

resource "confluent_role_binding" "topic-airline_skyone-read" {
  principal   = "User:${confluent_service_account.kafka_cluster_api.id}"
  role_name   = "DeveloperRead"
  crn_pattern = "${confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${confluent_kafka_cluster.kafka_cluster.id}/topic=${confluent_kafka_topic.airline_skyone.topic_name}"
}

resource "confluent_kafka_topic" "airline_sunset" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "airline.sunset"
  partitions_count   = 1
  rest_endpoint      = confluent_kafka_cluster.kafka_cluster.rest_endpoint

  config = {
    "retention.bytes" = "-1"
    "retention.ms"    = "-1"
  }
  credentials {
    key    = module.kafka_cluster_api_key_rotation.active_api_key.id
    secret = module.kafka_cluster_api_key_rotation.active_api_key.secret
  }
}

resource "confluent_role_binding" "topic-airline_sunset-write" {
  principal   = "User:${confluent_service_account.kafka_cluster_api.id}"
  role_name   = "DeveloperWrite"
  crn_pattern = "${confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${confluent_kafka_cluster.kafka_cluster.id}/topic=${confluent_kafka_topic.airline_sunset.topic_name}"
}

resource "confluent_role_binding" "topic-airline_sunset-read" {
  principal   = "User:${confluent_service_account.kafka_cluster_api.id}"
  role_name   = "DeveloperRead"
  crn_pattern = "${confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${confluent_kafka_cluster.kafka_cluster.id}/topic=${confluent_kafka_topic.airline_sunset.topic_name}"
}


resource "confluent_kafka_topic" "airline_all" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "airline.all"
  partitions_count   = 1
  rest_endpoint      = confluent_kafka_cluster.kafka_cluster.rest_endpoint

  config = {
    "retention.bytes" = "-1"
    "retention.ms"    = "-1"
  }
  credentials {
    key    = module.kafka_cluster_api_key_rotation.active_api_key.id
    secret = module.kafka_cluster_api_key_rotation.active_api_key.secret
  }
}

resource "confluent_role_binding" "topic-airline_all-write" {
  principal   = "User:${confluent_service_account.kafka_cluster_api.id}"
  role_name   = "DeveloperWrite"
  crn_pattern = "${confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${confluent_kafka_cluster.kafka_cluster.id}/topic=${confluent_kafka_topic.airline_all.topic_name}"
}

resource "confluent_role_binding" "topic-airline_all-read" {
  principal   = "User:${confluent_service_account.kafka_cluster_api.id}"
  role_name   = "DeveloperRead"
  crn_pattern = "${confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${confluent_kafka_cluster.kafka_cluster.id}/topic=${confluent_kafka_topic.airline_all.topic_name}"
}

resource "confluent_kafka_topic" "airline_user_statistics" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "airline.user_statistics"
  partitions_count   = 1
  rest_endpoint      = confluent_kafka_cluster.kafka_cluster.rest_endpoint

  config = {
    "retention.bytes" = "-1"
    "retention.ms"    = "-1"
  }
  credentials {
    key    = module.kafka_cluster_api_key_rotation.active_api_key.id
    secret = module.kafka_cluster_api_key_rotation.active_api_key.secret
  }
}

resource "confluent_role_binding" "topic-user_statistics-write" {
  principal   = "User:${confluent_service_account.kafka_cluster_api.id}"
  role_name   = "DeveloperWrite"
  crn_pattern = "${confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${confluent_kafka_cluster.kafka_cluster.id}/topic=${confluent_kafka_topic.user_statistics.topic_name}"
}

resource "confluent_role_binding" "topic-user_statistics-read" {
  principal   = "User:${confluent_service_account.kafka_cluster_api.id}"
  role_name   = "DeveloperRead"
  crn_pattern = "${confluent_kafka_cluster.kafka_cluster.rbac_crn}/kafka=${confluent_kafka_cluster.kafka_cluster.id}/topic=${confluent_kafka_topic.user_statistics.topic_name}"
}
