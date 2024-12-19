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
    confluent_api_key.flink_sql_statements_runner_api_key,
  ]
}

# Create the Environment API Key Pairs, rotate them in accordance to a time schedule, and provide the current
# acitve API Key Pair to use
module "flink_api_key_rotation" {
    
    source  = "github.com/j3-signalroom/iac-confluent-api_key_rotation-tf_module"

    # Required Input(s)
    owner = {
        id          = confluent_service_account.flink_sql_statements_runner.id
        api_version = confluent_service_account.flink_sql_statements_runner.api_version
        kind        = confluent_service_account.flink_sql_statements_runner.kind
    }

    resource = {
        id          = data.confluent_flink_region.env.id
        api_version = data.confluent_flink_region.env.api_version
        kind        = data.confluent_flink_region.env.kind

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
resource "confluent_api_key" "flink_sql_statements_runner_api_key" {
  display_name = "flink-sql-statements-runner-api-key"
  description  = "Flink API Key that is owned by 'flink_sql_statements_runner' service account"
  owner {
    id          = confluent_service_account.flink_sql_statements_runner.id
    api_version = confluent_service_account.flink_sql_statements_runner.api_version
    kind        = confluent_service_account.flink_sql_statements_runner.kind
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
resource "confluent_flink_statement" "create_flight_avro_ccaf_table" {

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

  statement = file("flink_sql_statements/create_flight_avro_ccaf_table.fql")

  rest_endpoint = data.confluent_flink_region.env.rest_endpoint

  properties = {
    "sql.current-catalog"  = confluent_environment.env.display_name
    "sql.current-database" = confluent_kafka_cluster.kafka_cluster.display_name
  }

  credentials {
    key    = confluent_api_key.flink_sql_statements_runner_api_key.id
    secret = confluent_api_key.flink_sql_statements_runner_api_key.secret
  }

  depends_on = [
    confluent_api_key.flink_sql_statements_runner_api_key,
    confluent_kafka_cluster.kafka_cluster
  ]
}
