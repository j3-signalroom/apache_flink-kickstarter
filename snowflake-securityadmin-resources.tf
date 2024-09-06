provider "snowflake" {
  alias         = "security_admin"
  role          = "SECURITYADMIN"
  account       = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"]
  user          = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["admin_user"]
  authenticator = "JWT"
  private_key   = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["active_rsa_public_key_number"] == 1 ? data.aws_secretsmanager_secret_version.admin_private_key_1.secret_string : data.aws_secretsmanager_secret_version.admin_private_key_2.secret_string
}

resource "snowflake_account_role" "role" {
  provider = snowflake.security_admin
  name     = "${local.secrets_insert}_role"
}

resource "snowflake_grant_privileges_to_account_role" "database_grant" {
  provider          = snowflake.security_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.role.name
  on_account_object {
    object_type = "DATABASE"
    object_name = snowflake_database.example.name
  }
}

resource "snowflake_schema" "schema" {
  database   = snowflake_database.example.name
  name       = local.secrets_insert
}

resource "snowflake_grant_privileges_to_account_role" "schema_grant" {
  provider          = snowflake.security_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.role.name
  on_schema {
    schema_name = "\"${snowflake_database.example.name}\".\"${snowflake_schema.schema.name}\""
  }
}

resource "snowflake_grant_privileges_to_account_role" "warehouse_grant" {
  provider          = snowflake.security_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.role.name
  on_account_object {
    object_type = "WAREHOUSE"
    object_name = snowflake_warehouse.example.name
  }
}

resource "snowflake_user" "user" {
  provider          = snowflake.security_admin
  name              = var.service_account_user
  default_warehouse = snowflake_warehouse.example.name
  default_role      = snowflake_account_role.role.name
  default_namespace = "${snowflake_database.example.name}.${snowflake_schema.schema.name}"

  # Setting the attributes to `null`, effectively unsets the attribute
  # Refer to this link `https://docs.snowflake.com/en/user-guide/key-pair-auth#configuring-key-pair-rotation`
  # for more information
  rsa_public_key    = module.snowflake_user_rsa_key_pairs_rotation.active_rsa_public_key_number == 1 ? jsondecode(data.aws_secretsmanager_secret_version.svc_public_keys.secret_string)["rsa_public_key_1"] : null
  rsa_public_key_2  = module.snowflake_user_rsa_key_pairs_rotation.active_rsa_public_key_number == 2 ? jsondecode(data.aws_secretsmanager_secret_version.svc_public_keys.secret_string)["rsa_public_key_2"] : null
}

resource "snowflake_grant_privileges_to_account_role" "user_grant" {
  provider          = snowflake.security_admin
  privileges        = ["MONITOR"]
  account_role_name = snowflake_account_role.role.name  
  on_account_object {
    object_type = "USER"
    object_name = snowflake_user.user.name
  }
}

resource "snowflake_grant_account_role" "grants" {
  provider  = snowflake.security_admin
  role_name = snowflake_account_role.role.name
  user_name = snowflake_user.user.name
}
