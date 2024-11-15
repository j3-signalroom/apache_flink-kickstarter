
# Reference the Confluent Cloud
data "confluent_organization" "env" {}

# Create the Confluent Cloud Environment
resource "confluent_environment" "env" {
  display_name = "${local.secrets_insert}"

  stream_governance {
    package = "ESSENTIALS"
  }
}

# Service account to perform the task within Confluent Cloud to execute the Flink SQL statements
resource "confluent_service_account" "flink_sql_statements_runner" {
  display_name = "flink-sql-statements-runner"
  description  = "Service account for running Flink SQL Statements in the Kafka cluster"
}

resource "confluent_role_binding" "flink_sql_statements_runner_env_admin" {
  principal   = "User:${confluent_service_account.flink_sql_statements_runner.id}"
  role_name   = "EnvironmentAdmin"
  crn_pattern = confluent_environment.env.resource_name
}

# Service account that owns Flink API Key
resource "confluent_service_account" "flink_app_manager" {
  display_name = "flink_app_manager"
  description  = "Service account that has got full access to Flink resources in an environment"
}

# https://docs.confluent.io/cloud/current/access-management/access-control/rbac/predefined-rbac-roles.html#flinkadmin
resource "confluent_role_binding" "flink_app_manager_developer_rbac" {
  principal   = "User:${confluent_service_account.flink_app_manager.id}"
  role_name   = "FlinkAdmin"
  crn_pattern = confluent_environment.env.resource_name
}

/*
  https://docs.confluent.io/cloud/current/access-management/access-control/rbac/predefined-rbac-roles.html#assigner
  https://docs.confluent.io/cloud/current/flink/operate-and-deploy/flink-rbac.html#submit-long-running-statements
*/
resource "confluent_role_binding" "flink_app_manager_assigner" {
  principal   = "User:${confluent_service_account.flink_app_manager.id}"
  role_name   = "Assigner"
  crn_pattern = "${data.confluent_organization.env.resource_name}/service-account=${confluent_service_account.flink_sql_statements_runner.id}"
}


data "confluent_flink_region" "env" {
  cloud        = local.cloud
  region       = var.aws_region
}

# https://docs.confluent.io/cloud/current/flink/get-started/quick-start-cloud-console.html#step-1-create-a-af-compute-pool
resource "confluent_flink_compute_pool" "env" {
  display_name = "flink_sql_statement_runner"
  cloud        = local.cloud
  region       = var.aws_region
  max_cfu      = 10
  environment {
    id = confluent_environment.env.id
  }
  depends_on = [
    confluent_role_binding.flink_sql_statements_runner_env_admin,
    confluent_role_binding.flink_app_manager_assigner,
    confluent_role_binding.flink_app_manager_developer_rbac,
    confluent_api_key.flink_app_manager_api_key,
  ]
}

# Create the Environment API Key Pairs, rotate them in accordance to a time schedule, and provide the current
# acitve API Key Pair to use
module "flink_api_key_rotation" {
    
    source  = "github.com/j3-signalroom/iac-confluent-api_key_rotation-tf_module"

    # Required Input(s)
    owner = {
        id          = confluent_service_account.flink_app_manager.id
        api_version = confluent_service_account.flink_app_manager.api_version
        kind        = confluent_service_account.flink_app_manager.kind
    }

    resource = {
        id          = confluent_flink_compute_pool.env.id
        api_version = confluent_flink_compute_pool.env.api_version
        kind        = confluent_flink_compute_pool.env.kind

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

# Create the Flink-specific API key that will be used to submit statements.
resource "confluent_api_key" "flink_app_manager_api_key" {
  display_name = "app-manager-flink-api-key"
  description  = "Flink API Key that is owned by 'flink_app_manager' service account"
  owner {
    id          = confluent_service_account.flink_app_manager.id
    api_version = confluent_service_account.flink_app_manager.api_version
    kind        = confluent_service_account.flink_app_manager.kind
  }
  managed_resource {
    id          = data.confluent_flink_region.env.id
    api_version = data.confluent_flink_region.env.api_version
    kind        = data.confluent_flink_region.env.kind
    
    environment {
      id = confluent_environment.env.id
    }
  }

  depends_on = [
    confluent_environment.env,
    confluent_service_account.flink_sql_statements_runner
  ]
}

# Create Airline Flights Table
resource "confluent_flink_statement" "create_airline_flights_table" {
  organization {
    id = data.confluent_organization.env.id
  }

  environment {
    id = confluent_environment.env.id
  }

  compute_pool {
    id = confluent_flink_compute_pool.env.id
  }

  principal {
    id = confluent_service_account.flink_sql_statements_runner.id
  }

  statement = file("flink_sql_statements/create_airline_flights_table.fql")

  rest_endpoint = data.confluent_flink_region.env.rest_endpoint

  properties = {
    "sql.current-catalog"  = confluent_environment.env.display_name
    "sql.current-database" = confluent_kafka_cluster.kafka_cluster.display_name
  }

  credentials {
    key    = confluent_api_key.flink_app_manager_api_key.id
    secret = confluent_api_key.flink_app_manager_api_key.secret
  }

  depends_on = [
    confluent_api_key.flink_app_manager_api_key,
    confluent_kafka_cluster.kafka_cluster
  ]
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
    display_name = local.secrets_insert
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
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

  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster 
  ]
}

# Create the `airline.flight` Kafka topic
resource "confluent_kafka_topic" "airline_flight" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "airline.flight"
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

# Create the `airline.flyer_stats` Kafka topic
resource "confluent_kafka_topic" "airline_flyer_stats" {
  kafka_cluster {
    id = confluent_kafka_cluster.kafka_cluster.id
  }
  topic_name         = "airline.flyer_stats"
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
