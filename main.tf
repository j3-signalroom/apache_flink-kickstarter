terraform {
    cloud {
      organization = "signalroom"

        workspaces {
            name = "apache-flink-kickstarter-0007"
        }
  }

  # Using the "pessimistic constraint operators" for all the Providers to ensure
  # that the provider version is compatible with the configuration.  Meaning
  # only patch-level updates are allowed but minor-level and major-level 
  # updates of the Providers are not allowed
  required_providers {
        confluent = {
            source  = "confluentinc/confluent"
            version = "~> 2.11.0"
        }
        aws = {
            source  = "hashicorp/aws"
            version = "~> 5.78.0"
        }
        snowflake = {
            source = "Snowflake-Labs/snowflake"
            version = "~> 0.99.0"
        }
    }
}

locals {
  cloud                         = "AWS"
  secrets_insert                = lower(var.service_account_user)
  confluent_secrets_path_prefix = "/confluent_cloud_resource/${local.secrets_insert}"
  snowflake_secrets_path_prefix = "/snowflake_resource/${local.secrets_insert}"
}


# Create the Snowflake user RSA keys pairs
module "snowflake_user_rsa_key_pairs_rotation" {   
    source  = "github.com/j3-signalroom/iac-snowflake-user-rsa_key_pairs_rotation-tf_module"

    # Required Input(s)
    aws_region           = var.aws_region
    aws_account_id       = var.aws_account_id
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