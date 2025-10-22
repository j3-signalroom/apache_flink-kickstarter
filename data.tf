data "aws_caller_identity" "current" {}

locals {
    cloud                           = "AWS"
    secrets_insert                  = lower(var.service_account_user)
    generic_name                    = "${upper(var.service_account_user)}"
    catalog_integration_name        = "${local.generic_name}_CATALOG_INTEGRATION"
    volume_name                     = "${local.generic_name}_VOLUME"
    user_name                       = "${local.generic_name}_USER"
    warehouse_name                  = "${local.generic_name}_WAREHOUSE"
    database_name                   = "${local.generic_name}_DATABASE"
    schema_name                     = "${local.generic_name}_SCHEMA"
    location_name                   = "${local.generic_name}_LOCATION"
    security_admin_role             = "${local.generic_name}_SECURITY_ADMIN_ROLE"
    system_admin_role               = "${local.generic_name}_SYSTEM_ADMIN_ROLE"
    confluent_secrets_path_prefix   = "/confluent_cloud_resource/${local.secrets_insert}"
    snowflake_secrets_path_prefix   = "/snowflake_resource/${local.secrets_insert}"
    snowflake_aws_role_name         = "snowflake_role"
    snowflake_aws_role_arn          = "arn:aws:iam::${var.aws_account_id}:role/${local.snowflake_aws_role_name}"
    s3_bucket_warehouse_name        = "s3://${aws_s3_bucket.iceberg_bucket.bucket}/${aws_s3_object.warehouse.key}"
    snowflake_aws_s3_glue_role_name = "snowflake_s3_glue_role"
    snowflake_aws_s3_glue_role_arn  = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${local.snowflake_aws_s3_glue_role_name}"

    # Snowflake connection details from Secrets Manager
    snowflake_account_identifier    = jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_account_identifier"]
    snowflake_organization_name     = jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_organization_name"]
    snowflake_account_name          = jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_account_name"]
    snowflake_admin_service_user    = jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["admin_service_user"]
    snowflake_active_private_key    = base64decode(jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["active_key_number"] == 1 ? jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_rsa_private_key_1_pem"] : jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_rsa_private_key_2_pem"])
}  
