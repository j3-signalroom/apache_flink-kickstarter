provider "snowflake" {
  alias = "account_admin"
  role  = "ACCOUNTADMIN"

  # The most recently version of Snowflake Terraform Provider requires the 
  # `organization_name` and `account_name` to be set, whereas the previous 
  # versions did not require this.  That is why we are setting these values
  # here.  Plus, `account` as been deprecated in favor of `account_name`.
  organization_name = "${split("-", jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"])[0]}"
  account_name      = "${split("-", jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"])[1]}"
  user              = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["admin_user"]
  authenticator     = "JWT"
  private_key       = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["active_rsa_public_key_number"] == 1 ? data.aws_secretsmanager_secret_version.admin_private_key_1.secret_string : data.aws_secretsmanager_secret_version.admin_private_key_2.secret_string
}

resource "snowflake_account_role" "account_admin_role" {
  provider = snowflake.account_admin
  name     = "${local.secrets_insert}_account_admin_role"
}

resource "snowflake_grant_privileges_to_account_role" "integration_grant" {
  provider          = snowflake.securiaccount_adminty_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.account_admin.name
  on_account_object {
    object_type = "INTEHGRATION"
    object_name = snowflake_storage_integration.aws_s3_integration.name
  }
}

resource "snowflake_storage_integration" "aws_s3_integration" {
  provider                  = snowflake.account_admin
  name                      = "AWS_S3_STORAGE_INTEGRATION"
  storage_allowed_locations = ["s3://flink-kickstarter/warehouse/airlines.db/"]
  storage_provider          = "S3"
  storage_aws_object_acl    = "bucket-owner-full-control"
  storage_aws_role_arn      = aws_iam_role.snowflake_role.arn
  enabled                   = true
  type                      = "EXTERNAL_STAGE"

  depends_on = [ 
    aws_iam_role.snowflake_role
  ]
}