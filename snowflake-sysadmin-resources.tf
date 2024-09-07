provider "snowflake" {
  role          = "SYSADMIN"
  account       = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"]
  user          = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["admin_user"]
  authenticator = "JWT"
  private_key   = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["active_rsa_public_key_number"] == 1 ? data.aws_secretsmanager_secret_version.admin_private_key_1.secret_string : data.aws_secretsmanager_secret_version.admin_private_key_2.secret_string
}

resource "snowflake_database" "example" {
  name = local.secrets_insert
}

resource "snowflake_warehouse" "example" {
  name           = local.secrets_insert
  warehouse_size = "xsmall"
  auto_suspend   = 60
}