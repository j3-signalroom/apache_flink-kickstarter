terraform {
    cloud {
      organization = "signalroom"

        workspaces {
            name = "apache-flink-kickstarter-0015"
        }
  }

  # Using the "pessimistic constraint operators" for all the Providers to ensure
  # that the provider version is compatible with the configuration.  Meaning
  # only patch-level updates are allowed but minor-level and major-level 
  # updates of the Providers are not allowed
  required_providers {
        confluent = {
            source  = "confluentinc/confluent"
            version = "2.32.0"
        }
        aws = {
            source  = "hashicorp/aws"
            version = "6.17.0"
        }
        snowflake = {
            source = "snowflakedb/snowflake"
            version = "2.1.0"
        }
    }
}

locals {
  cloud                         = "AWS"
  secrets_insert                = lower(var.service_account_user)
  confluent_secrets_path_prefix = "/confluent_cloud_resource/${local.secrets_insert}"
  snowflake_secrets_path_prefix = "/snowflake_resource/${local.secrets_insert}"
  snowflake_aws_role_name       = "snowflake_role"
  snowflake_aws_role_arn        = "arn:aws:iam::${var.aws_account_id}:role/${local.snowflake_aws_role_name}"
}


# Create the Snowflake user RSA keys pairs
module "snowflake_user_rsa_key_pairs_rotation" {   
    source  = "github.com/j3-signalroom/iac-snowflake-user-rsa_key_pairs_rotation-tf_module"

    # Required Input(s)
    aws_region           = var.aws_region
    snowflake_account    = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"]
    service_account_user = var.service_account_user

    # Optional Input(s)
    secret_insert             = local.secrets_insert
    day_count                 = var.day_count
    aws_lambda_memory_size    = var.aws_lambda_memory_size
    aws_lambda_timeout        = var.aws_lambda_timeout
    aws_log_retention_in_days = var.aws_log_retention_in_days
}

# Reference the Confluent Cloud
data "confluent_organization" "env" {}

# Create the Confluent Cloud Environment
resource "confluent_environment" "env" {
  display_name = "${local.secrets_insert}"

  stream_governance {
    package = "ESSENTIALS"
  }
}