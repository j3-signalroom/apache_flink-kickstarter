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
