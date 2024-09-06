
# Reference the Confluent Cloud
data "confluent_organization" "env" {}

# Create the Confluent Cloud Environment
resource "confluent_environment" "env" {
  display_name = "${local.secrets_insert}"

  stream_governance {
    package = "ESSENTIALS"
  }
}


# Create the Service Account for the Kafka Cluster API
resource "confluent_service_account" "schema_registry_cluster_api" {
    display_name = "${local.secrets_insert}-environment-api"
    description  = "Environment API Service Account"
}

# Config the environment's schema registry
data "confluent_schema_registry_cluster" "env" {
  environment {
    id = confluent_environment.env.id
  }

  depends_on = [
    confluent_kafka_cluster.kafka_cluster
  ]
}

# Create the Environment API Key Pairs, rotate them in accordance to a time schedule, and provide the current
# acitve API Key Pair to use
module "schema_registry_cluster_api_key_rotation" {
    
    source  = "github.com/j3-signalroom/iac-confluent-api_key_rotation-tf_module"

    # Required Input(s)
    owner = {
        id          = confluent_service_account.schema_registry_cluster_api.id
        api_version = confluent_service_account.schema_registry_cluster_api.api_version
        kind        = confluent_service_account.schema_registry_cluster_api.kind
    }

    resource = {
        id          = data.confluent_schema_registry_cluster.env.id
        api_version = data.confluent_schema_registry_cluster.env.api_version
        kind        = data.confluent_schema_registry_cluster.env.kind

        environment = {
            id = confluent_environment.env.id
        }
    }

    confluent_api_key    = var.confluent_api_key
    confluent_api_secret = var.confluent_api_secret

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
    display_name = "${local.secrets_insert}-kafka_cluster-api"
    description  = "Kafka Cluster API Service Account"
}

# Since the Kafka Cluster created is a Basic Cluster type, setting more granular 
# permissions is not allowed. Therefore, the ‘EnvironmentAdmin’ role is assigned
# to the entire cluster instead of implementing RBAC for specific Kafka topics.
resource "confluent_role_binding" "kafka_cluster_api_environment_admin" {
  principal   = "User:${confluent_service_account.kafka_cluster_api.id}"
  role_name   = "EnvironmentAdmin"
  crn_pattern = confluent_environment.env.resource_name
}

# Create the Kafka Cluster API Key Pairs, rotate them in accordance to a time schedule,
# and provide the current acitve API Key Pair to use
module "kafka_cluster_api_key_rotation" {
    source  = "github.com/j3-signalroom/iac-confluent-api_key_rotation-tf_module"

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

    confluent_api_key    = var.confluent_api_key
    confluent_api_secret = var.confluent_api_secret

    # Optional Input(s)
    key_display_name = "Confluent Kafka Cluster Service Account API Key - {date} - Managed by Terraform Cloud"
    number_of_api_keys_to_retain = var.number_of_api_keys_to_retain
    day_count = var.day_count
}

# Create the `airline.skyone` Kafka topic
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

# Create the `airline.sunset` Kafka topic
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

# Create the `airline.all` Kafka topic
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

# Create the `airline.user_statistics` Kafka topic
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