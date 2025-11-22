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
            version = "2.51.0"
        }
        aws = {
            source  = "hashicorp/aws"
            version = "6.21.0"
        }
        snowflake = {
            source = "snowflakedb/snowflake"
            version = "2.11.0"
        }
    }
}
