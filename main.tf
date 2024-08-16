terraform {
    cloud {
      organization = "signalroom"
      #organization = "<TERRAFORM CLOUD ORGANIZATION NAME>"

        workspaces {
            name = "confluent-cloud-us-east-005"
            #name = "<TERRAFORM CLOUD ORGANIZATION's WORKSPACE NAME>"
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
  cloud          = "AWS"
  secrets_prefix = "/confluent_cloud_resource"
}
