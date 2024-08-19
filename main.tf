terraform {
    cloud {
      organization = "<TERRAFORM CLOUD ORGANIZATION NAME>"

        workspaces {
            name = "<TERRAFORM CLOUD ORGANIZATION's WORKSPACE NAME>"
        }
  }

  required_providers {
        confluent = {
            source  = "confluentinc/confluent"
            version = "~> 1.82.0"
        }
        aws = {
            source  = "hashicorp/aws"
            version = "~> 5.60.0"
        }
    }
}

locals {
  cloud                          = "AWS"
  confluent_cloud_secrets_prefix = "/confluent_cloud_resource"
  snowflake_secrets_prefix       = "/snowflake_resource"
}
