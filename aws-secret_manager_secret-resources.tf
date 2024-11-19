resource "aws_secretsmanager_secret" "schema_registry_cluster_api_key_java_client" {
    name = "${local.confluent_secrets_path_prefix}/schema_registry_cluster/java_client"
    description = "Schema Registry Cluster secrets for Java client"
}
resource "aws_secretsmanager_secret_version" "schema_registry_cluster_api_key_java_client" {
    secret_id     = aws_secretsmanager_secret.schema_registry_cluster_api_key_java_client.id
    secret_string = jsonencode({"schema.registry.basic.auth.credentials.source": "USER_INFO",
                                "schema.registry.basic.auth.user.info": "${module.schema_registry_cluster_api_key_rotation.active_api_key.id}:${module.schema_registry_cluster_api_key_rotation.active_api_key.secret}",
                                "schema.registry.url": "${data.confluent_schema_registry_cluster.env.rest_endpoint}"})
}

# Create the Schema Registry Cluster Secrets: API Key Pair and REST endpoint for Python client
resource "aws_secretsmanager_secret" "schema_registry_cluster_api_key_python_client" {
    name = "${local.confluent_secrets_path_prefix}/schema_registry_cluster/python_client"
    description = "Schema Registry Cluster secrets for Python client"
}
resource "aws_secretsmanager_secret_version" "schema_registry_cluster_api_key_python_client" {
    secret_id     = aws_secretsmanager_secret.schema_registry_cluster_api_key_python_client.id
    secret_string = jsonencode({"schema.registry.basic.auth.credentials.source": "USER_INFO",
                                "schema.registry.basic.auth.user.info": "${module.schema_registry_cluster_api_key_rotation.active_api_key.id}:${module.schema_registry_cluster_api_key_rotation.active_api_key.secret}",
                                "schema.registry.url": "${data.confluent_schema_registry_cluster.env.rest_endpoint}"})
}

# Create the Kafka Cluster Secrets: API Key Pair, JAAS (Java Authentication and Authorization) representation
# for Java client, bootstrap server URI and REST endpoint
resource "aws_secretsmanager_secret" "kafka_cluster_api_key_java_client" {
    name = "${local.confluent_secrets_path_prefix}/kafka_cluster/java_client"
    description = "Kafka Cluster secrets for Java client"
}
resource "aws_secretsmanager_secret_version" "kafka_cluster_api_key_java_client" {
    secret_id     = aws_secretsmanager_secret.kafka_cluster_api_key_java_client.id
    secret_string = jsonencode({"sasl.jaas.config": "org.apache.kafka.common.security.plain.PlainLoginModule required username='${module.kafka_cluster_api_key_rotation.active_api_key.id}' password='${module.kafka_cluster_api_key_rotation.active_api_key.secret}';",
                                "bootstrap.servers": replace(confluent_kafka_cluster.kafka_cluster.bootstrap_endpoint, "SASL_SSL://", "")})
}

# Create the Kafka Cluster Secrets: API Key Pair, JAAS (Java Authentication and Authorization) representation
# for Python client, bootstrap server URI and REST endpoint
resource "aws_secretsmanager_secret" "kafka_cluster_api_key_python_client" {
    name = "${local.confluent_secrets_path_prefix}/kafka_cluster/python_client"
    description = "Kafka Cluster secrets for Python client"
}

resource "aws_secretsmanager_secret_version" "kafka_cluster_api_key_python_client" {
    secret_id     = aws_secretsmanager_secret.kafka_cluster_api_key_python_client.id
    secret_string = jsonencode({"sasl.username": "${module.kafka_cluster_api_key_rotation.active_api_key.id}",
                                "sasl.password": "${module.kafka_cluster_api_key_rotation.active_api_key.secret}",
                                "bootstrap.servers": replace(confluent_kafka_cluster.kafka_cluster.bootstrap_endpoint, "SASL_SSL://", "")})
}

# Create the Flink Compute Pool: API Key Pair
resource "aws_secretsmanager_secret" "flink_compute_pool" {
    name = "${local.confluent_secrets_path_prefix}/flink_compute_pool"
    description = "Confluent Cloud Apache Flink secrets"
}

resource "aws_secretsmanager_secret_version" "flink_compute_pool" {
    secret_id     = aws_secretsmanager_secret.flink_compute_pool.id
    secret_string = jsonencode({"flink.cloud": "${local.cloud}",
                                "flink.region": "${var.aws_region}",
                                "flink.api.key": "${module.flink_api_key_rotation.active_api_key.id}",
                                "flink.api.secret": "${module.flink_api_key_rotation.active_api_key.secret}",
                                "organzation.id": "${data.confluent_organization.env.id}",
                                "environment.id": "${confluent_environment.env.id}",
                                "flink.compute.pool.id": "${confluent_flink_compute_pool.env.id}"})
}

data "aws_secretsmanager_secret" "admin_public_keys" {
  name = "/snowflake_admin_credentials"
}

data "aws_secretsmanager_secret_version" "admin_public_keys" {
  secret_id = data.aws_secretsmanager_secret.admin_public_keys.id
}

data "aws_secretsmanager_secret" "admin_private_key_1" {
  name = "/snowflake_admin_credentials/rsa_private_key_pem_1"
}

data "aws_secretsmanager_secret_version" "admin_private_key_1" {
  secret_id = data.aws_secretsmanager_secret.admin_private_key_1.id
}

data "aws_secretsmanager_secret" "admin_private_key_2" {
  name = "/snowflake_admin_credentials/rsa_private_key_pem_2"
}

data "aws_secretsmanager_secret_version" "admin_private_key_2" {
  secret_id = data.aws_secretsmanager_secret.admin_private_key_2.id
}

data "aws_secretsmanager_secret" "svc_public_keys" {
  name = local.snowflake_secrets_path_prefix

  depends_on = [ 
    module.snowflake_user_rsa_key_pairs_rotation 
  ]
}

data "aws_secretsmanager_secret_version" "svc_public_keys" {
  secret_id = data.aws_secretsmanager_secret.svc_public_keys.id
}
