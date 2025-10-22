provider "confluent" {
  cloud_api_key    = var.confluent_api_key
  cloud_api_secret = var.confluent_api_secret
}

provider "aws" {
    region     = var.aws_region
    access_key = var.aws_access_key_id
    secret_key = var.aws_secret_access_key
    token      = var.aws_session_token
}

provider "snowflake" {
  role              = "SYSADMIN"
  organization_name = local.snowflake_organization_name
  account_name      = local.snowflake_account_name
  user              = local.snowflake_admin_service_user
  private_key       = local.snowflake_active_private_key
  authenticator     = "SNOWFLAKE_JWT"
}

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

provider "snowflake" {
  alias = "account_admin"
  role  = "ACCOUNTADMIN"

  organization_name           = local.snowflake_organization_name
  account_name                = local.snowflake_account_name
  user                        = local.snowflake_admin_service_user
  private_key                 = local.snowflake_active_private_key
  authenticator               = "SNOWFLAKE_JWT"
  validate_default_parameters = false
  warehouse                   = local.warehouse_name

  preview_features_enabled = [
    "snowflake_external_volume_resource"
  ]
}
