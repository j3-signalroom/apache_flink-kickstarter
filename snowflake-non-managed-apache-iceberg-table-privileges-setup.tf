provider "snowflake" {
  alias                       = "security_admin"
  role                        = "SECURITYADMIN"
  organization_name           = local.snowflake_organization_name
  account_name                = local.snowflake_account_name
  user                        = local.snowflake_admin_service_user
  private_key                 = local.snowflake_active_private_key
  authenticator               = "SNOWFLAKE_JWT"
  validate_default_parameters = false
}

resource "snowflake_account_role" "security_admin_role" {
  provider = snowflake.security_admin
  name     = "${upper(local.secrets_insert)}_ROLE"
}

resource "snowflake_grant_privileges_to_account_role" "warehouse" {
  provider          = snowflake.security_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.security_admin_role.name
  on_account_object {
    object_type = "WAREHOUSE"
    object_name = snowflake_warehouse.apache_flink.name
  }
}

resource "snowflake_user" "user" {
  provider          = snowflake.security_admin
  name              = upper(var.service_account_user)
  default_warehouse = snowflake_warehouse.apache_flink.name
  default_role      = snowflake_account_role.security_admin_role.name
  default_namespace = "${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}"

  # Setting the attributes to `null`, effectively unsets the attribute
  # Refer to this link `https://docs.snowflake.com/en/user-guide/key-pair-auth#configuring-key-pair-rotation`
  # for more information
  rsa_public_key    = module.snowflake_service_user_rsa_key_pairs_rotation.active_key_number == 1 ? module.snowflake_service_user_rsa_key_pairs_rotation.snowflake_rsa_public_key_1_pem : null
  rsa_public_key_2  = module.snowflake_service_user_rsa_key_pairs_rotation.active_key_number == 2 ? module.snowflake_service_user_rsa_key_pairs_rotation.snowflake_rsa_public_key_2_pem : null
}

resource "snowflake_grant_privileges_to_account_role" "user" {
  provider          = snowflake.security_admin
  privileges        = ["MONITOR"]
  account_role_name = snowflake_account_role.security_admin_role.name  
  on_account_object {
    object_type = "USER"
    object_name = snowflake_user.user.name
  }
}

resource "snowflake_grant_account_role" "user_security_admin" {
  provider  = snowflake.security_admin
  role_name = snowflake_account_role.security_admin_role.name
  user_name = snowflake_user.user.name
}

provider "snowflake" {
  alias = "account_admin"
  role  = "ACCOUNTADMIN"

  organization_name = local.snowflake_organization_name
  account_name      = local.snowflake_account_name
  user              = local.snowflake_admin_service_user
  private_key       = local.snowflake_active_private_key
  authenticator     = "SNOWFLAKE_JWT"

  # Enable preview features
  preview_features_enabled = [
    "snowflake_storage_integration_resource",
    "snowflake_file_format_resource",
    "snowflake_stage_resource",
    "snowflake_external_table_resource"
  ]
}

resource "snowflake_account_role" "account_admin_role" {
  provider = snowflake.account_admin
  name     = "${upper(local.secrets_insert)}_ACCOUNT_ADMIN_ROLE"
}

resource "snowflake_grant_privileges_to_account_role" "database" {
  provider          = snowflake.account_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.account_admin_role.name
  on_account_object {
    object_type = "DATABASE"
    object_name = snowflake_database.apache_flink.name
  }
}

resource "snowflake_grant_privileges_to_account_role" "schema" {
  provider          = snowflake.account_admin
  privileges        = ["CREATE FILE FORMAT", "USAGE"]
  account_role_name = snowflake_account_role.account_admin_role.name
  on_schema {
    schema_name = "${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}"
  }
}

resource "snowflake_grant_privileges_to_account_role" "integration_grant" {
  provider          = snowflake.account_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.account_admin_role.name
  on_account_object {
    object_type = "INTEGRATION"
    object_name = snowflake_storage_integration.aws_s3_integration.name
  }
}

resource "snowflake_grant_account_role" "user_account_admin" {
  provider  = snowflake.account_admin
  role_name = snowflake_account_role.account_admin_role.name
  user_name = snowflake_user.user.name
}
