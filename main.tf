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
            version = "2.54.0"
        }
        aws = {
            source  = "hashicorp/aws"
            version = "6.22.1"
        }
        snowflake = {
            source = "snowflakedb/snowflake"
            version = "2.11.0"
        }
    }
}
