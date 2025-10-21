data "aws_secretsmanager_secret" "admin_service_user" {
  name = var.admin_service_user_secrets_root_path
}

data "aws_secretsmanager_secret_version" "admin_service_user" {
  secret_id = data.aws_secretsmanager_secret.admin_service_user.id
}

locals {
  # Snowflake connection details from Secrets Manager
  snowflake_account_identifier    = jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_account_identifier"]
  snowflake_organization_name     = jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_organization_name"]
  snowflake_account_name          = jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_account_name"]
  snowflake_admin_service_user    = jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["admin_service_user"]
  snowflake_active_private_key    = base64decode(jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["active_key_number"] == 1 ? jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_rsa_private_key_1_pem"] : jsondecode(data.aws_secretsmanager_secret_version.admin_service_user.secret_string)["snowflake_rsa_private_key_2_pem"])
}