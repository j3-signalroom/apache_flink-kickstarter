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

resource "confluent_schema_registry_acl" "developer_read" {
  subject_name = "*"
  principal    = "ServiceAccount:${confluent_service_account.schema_registry_cluster_api.id}"
  operation    = "READ"
  permission   = "ALLOW"

  depends_on = [ confluent_service_account.schema_registry_cluster_api ]
}

resource "confluent_schema_registry_acl" "developer_write" {
  subject_name = "*"
  principal    = "ServiceAccount:${confluent_service_account.schema_registry_cluster_api.id}"
  operation    = "WRITE"
  permission   = "ALLOW"

  depends_on = [ confluent_service_account.schema_registry_cluster_api ]
}