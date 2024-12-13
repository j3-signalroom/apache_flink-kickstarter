provider "snowflake" {
  alias = "account_admin"
  role  = "ACCOUNTADMIN"

  # Snowflake Terraform Provider 1.0.0 requires the `organization_name` and 
  # `account_name` to be set, whereas the previous versions did not require
  # this.  That is why we are setting these values here.  Plus, `account` as
  # been deprecated in favor of `account_name`.
  organization_name = "${split("-", jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"])[0]}"
  account_name      = "${split("-", jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"])[1]}"
  user              = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["admin_user"]
  authenticator     = "SNOWFLAKE_JWT"
  private_key       = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["active_rsa_public_key_number"] == 1 ? data.aws_secretsmanager_secret_version.admin_private_key_1.secret_string : data.aws_secretsmanager_secret_version.admin_private_key_2.secret_string

  # Enable preview features
  preview_features_enabled = [
    "snowflake_storage_integration_resource",
    "snowflake_file_format_resource"
  ]
}

resource "snowflake_account_role" "account_admin_role" {
  provider = snowflake.account_admin
  name     = "${local.secrets_insert}_account_admin_role"
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

resource "snowflake_grant_privileges_to_account_role" "skyone_airline_external_table" {
  provider          = snowflake.account_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.account_admin_role.name
  on_schema_object {
    object_type = "EXTERNAL TABLE"
    object_name = "${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}.${snowflake_external_table.skyone_airline.name}"
  }

  depends_on = [ 
    snowflake_account_role.account_admin_role,
    snowflake_external_table.skyone_airline
  ]
}

resource "snowflake_grant_privileges_to_account_role" "sunset_airline_external_table" {
  provider          = snowflake.account_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.account_admin_role.name
  on_schema_object {
    object_type = "EXTERNAL TABLE"
    object_name = "${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}.${snowflake_external_table.sunset_airline.name}"
  }

  depends_on = [ 
    snowflake_account_role.account_admin_role,
    snowflake_external_table.sunset_airline
  ]
}

resource "snowflake_grant_account_role" "user_account_admin" {
  provider  = snowflake.account_admin
  role_name = snowflake_account_role.account_admin_role.name
  user_name = snowflake_user.user.name
}

