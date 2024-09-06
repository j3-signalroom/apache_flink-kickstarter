terraform {
    cloud {
      organization = "signalroom"

        workspaces {
            name = "apache-flink-kictstarter"
        }
  }

  # Using the "pessimistic constraint operators" for all the Providers to ensure
  # that the provider version is compatible with the configuration.  Meaning
  # only patch-level updates are allowed but minor-level and major-level 
  # updates of the Providers are not allowed
  required_providers {
        confluent = {
            source  = "confluentinc/confluent"
            version = "~> 2.1.0"
        }
        aws = {
            source  = "hashicorp/aws"
            version = "~> 5.66.0"
        }
        snowflake = {
            source = "Snowflake-Labs/snowflake"
            version = "~> 0.95.0"
        }
    }
}

locals {
  cloud                          = "AWS"
  confluent_cloud_secrets_prefix = "/confluent_cloud_resource"
  snowflake_secrets_prefix       = "/snowflake_resource"
}
