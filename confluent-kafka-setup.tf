# Create the Kafka cluster
resource "confluent_kafka_cluster" "kafka_cluster" {
    display_name = "${local.secrets_insert}-kafka_cluster"
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

# Create the `skyone` Kafka topic
resource "confluent_kafka_topic" "airline_skyone" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "skyone"
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
}

# Create the `sunset` Kafka topic
resource "confluent_kafka_topic" "airline_sunset" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "sunset"
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
}

# Create the `flight` Kafka topic
resource "confluent_kafka_topic" "airline_flight" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "flight"
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
}

# Create the `flyer_stats` Kafka topic
resource "confluent_kafka_topic" "airline_flyer_stats" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "flyer_stats"
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
}

# Create the `skyone_avro` Kafka topic
resource "confluent_kafka_topic" "airline_skyone_avro" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "skyone_avro"
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
}

# Create the `sunset_avro` Kafka topic
resource "confluent_kafka_topic" "airline_sunset_avro" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "sunset_avro"
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
}

# Create the `flight` Kafka topic
resource "confluent_kafka_topic" "airline_flight_avro" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "flight_avro"
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
}

# Create the `flyer_stats_avro` Kafka topic
resource "confluent_kafka_topic" "airline_flyer_stats_avro" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "flyer_stats_avro"
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
}
