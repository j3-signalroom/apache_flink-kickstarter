terraform {
    cloud {
      organization = "signalroom"

        workspaces {
            name = "apache-flink-kickstarter"
        }
  }

  required_providers {
        confluent = {
            source  = "confluentinc/confluent"
            version = "2.50.0"
        }
        aws = {
            source  = "hashicorp/aws"
            version = "6.17.0"
        }
        snowflake = {
            source = "snowflakedb/snowflake"
            version = "2.9.0"
        }
    }
}
