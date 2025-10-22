resource "snowflake_warehouse" "warehouse" {
	name           = local.warehouse_name
	warehouse_size = "xsmall"
	auto_suspend   = 60
	provider       = snowflake
}

resource "snowflake_database" "database" {
	name     = local.database_name
	provider = snowflake

	depends_on = [ 
		snowflake_warehouse.warehouse
	]
}

resource "snowflake_schema" "schema" {
	name       = local.schema_name
	database   = local.database_name
	provider   = snowflake

	depends_on = [
		snowflake_database.database
	]
}

resource "snowflake_external_volume" "external_volume" {
	provider     = snowflake.account_admin
	name         = local.volume_name
	allow_writes = false
	storage_location {
		storage_location_name = "${local.secrets_insert}_LOCATION"
		storage_base_url      = local.s3_bucket_warehouse_name
		storage_provider      = "S3"
		storage_aws_role_arn  = local.snowflake_aws_s3_glue_role_arn
	}
}

# Snowflake Terraform Provider 2.9.0 does not support the creation of a catalog integration
resource "snowflake_execute" "catalog_integration" {
  provider = snowflake.account_admin
  depends_on = [ 
    confluent_kafka_cluster.kafka_cluster,
    snowflake_external_volume.tableflow_kickstarter_volume 
  ]

  execute = <<EOT
    CREATE OR REPLACE CATALOG INTEGRATION ${local.catalog_integration_name}
      CATALOG_SOURCE = GLUE
      TABLE_FORMAT = ICEBERG
      GLUE_AWS_ROLE_ARN = '${local.snowflake_aws_s3_glue_role_arn}'
      GLUE_CATALOG_ID = '${data.aws_caller_identity.current.account_id}'
      GLUE_REGION = '${var.aws_region}'
      CATALOG_NAMESPACE = 'airlines.db'
      ENABLED = TRUE;
  EOT

  revert = <<EOT
    DROP CATALOG INTEGRATION ${local.catalog_integration_name};
  EOT

  query = <<EOT
    DESCRIBE CATALOG INTEGRATION ${local.catalog_integration_name};
  EOT
}

resource "snowflake_external_table" "skyone_airline" {
  provider    = snowflake.account_admin
  database    = snowflake_database.apache_flink.name
  schema      = snowflake_schema.apache_flink_schema.name
  name        = upper("skyone_airline")
  file_format = "TYPE = 'PARQUET'"
  location    = "@${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}.${snowflake_stage.skyone_airline.name}"
  auto_refresh = true

  column {
    as   = "(value:email_address::string)"
    name = "email_address"
    type = "VARCHAR"
  }

  column {
    as   = "(value:departure_time::string)"
    name = "departure_time"
    type = "VARCHAR"
  }

  column {
    as   = "(value:departure_airport_code::string)"
    name = "departure_airport_code"
    type = "VARCHAR"
  }

  column {
    as   = "(value:arrival_time::string)"
    name = "arrival_time"
    type = "VARCHAR"
  }

  column {
    as   = "(value:arrival_airport_code::string)"
    name = "arrival_airport_code"
    type = "VARCHAR"
  }

  column {
    as   = "(value:flight_duration::bigint)"
    name = "flight_duration"
    type = "BIGINT"
  }

  column {
    as   = "(value:flight_number::string)"
    name = "flight_number"
    type = "VARCHAR"
  }

  column {
    as   = "(value:confirmation_code::string)"
    name = "confirmation_code"
    type = "VARCHAR"
  }

  column {
    as   = "(value:ticket_price::decimal(10, 2))"
    name = "ticket_price"
    type = "NUMBER"
  }

  column {
    as   = "(value:booking_agency_email::string)"
    name = "booking_agency_email"
    type = "VARCHAR"
  }

  depends_on = [ 
    snowflake_stage.skyone_airline
  ]
}

resource "snowflake_external_table" "sunset_airline" {
  provider    = snowflake.account_admin
  database    = snowflake_database.apache_flink.name
  schema      = snowflake_schema.apache_flink_schema.name
  name        = upper("sunset_airline")
  file_format = "TYPE = 'PARQUET'"
  location    = "@${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}.${snowflake_stage.sunset_airline.name}"
  auto_refresh = true

  column {
    as   = "(value:email_address::string)"
    name = "email_address"
    type = "VARCHAR"
  }

  column {
    as   = "(value:departure_time::string)"
    name = "departure_time"
    type = "VARCHAR"
  }

  column {
    as   = "(value:departure_airport_code::string)"
    name = "departure_airport_code"
    type = "VARCHAR"
  }

  column {
    as   = "(value:arrival_time::string)"
    name = "arrival_time"
    type = "VARCHAR"
  }

  column {
    as   = "(value:arrival_airport_code::string)"
    name = "arrival_airport_code"
    type = "VARCHAR"
  }

  column {
    as   = "(value:flight_duration::bigint)"
    name = "flight_duration"
    type = "BIGINT"
  }

  column {
    as   = "(value:flight_number::string)"
    name = "flight_number"
    type = "VARCHAR"
  }

  column {
    as   = "(value:confirmation_code::string)"
    name = "confirmation_code"
    type = "VARCHAR"
  }

  column {
    as   = "(value:ticket_price::decimal(10, 2))"
    name = "ticket_price"
    type = "NUMBER"
  }

  column {
    as   = "(value:booking_agency_email::string)"
    name = "booking_agency_email"
    type = "VARCHAR"
  }

  depends_on = [ 
    snowflake_stage.sunset_airline
  ]
}

resource "snowflake_external_table" "flight" {
  provider    = snowflake.account_admin
  database    = snowflake_database.apache_flink.name
  schema      = snowflake_schema.apache_flink_schema.name
  name        = upper("flight")
  file_format = "TYPE = 'PARQUET'"
  location    = "@${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}.${snowflake_stage.flight.name}"
  auto_refresh = true

  column {
    as   = "(value:email_address::string)"
    name = "email_address"
    type = "VARCHAR"
  }

  column {
    as   = "(value:departure_time::string)"
    name = "departure_time"
    type = "VARCHAR"
  }

  column {
    as   = "(value:departure_airport_code::string)"
    name = "departure_airport_code"
    type = "VARCHAR"
  }

  column {
    as   = "(value:arrival_time::string)"
    name = "arrival_time"
    type = "VARCHAR"
  }

  column {
    as   = "(value:arrival_airport_code::string)"
    name = "arrival_airport_code"
    type = "VARCHAR"
  }

  column {
    as   = "(value:flight_number::string)"
    name = "flight_number"
    type = "VARCHAR"
  }

  column {
    as   = "(value:confirmation_code::string)"
    name = "confirmation_code"
    type = "VARCHAR"
  }

  column {
    as   = "(value:airline::string)"
    name = "airline"
    type = "VARCHAR"
  }

  depends_on = [ 
    snowflake_stage.flight
  ]
}

resource "snowflake_stage" "flyer_stats" {
  name                = upper("flyer_stats_stage")
  url                 = "${local.s3_bucket_warehouse_name}/airlines.db/flyer_stats/data/"
  database            = snowflake_database.apache_flink.name
  schema              = snowflake_schema.apache_flink_schema.name
  storage_integration = snowflake_storage_integration.aws_s3_integration.name
  provider            = snowflake.account_admin

  depends_on = [ 
    snowflake_storage_integration.aws_s3_integration 
  ]
}

resource "snowflake_external_table" "flyer_stats" {
  provider    = snowflake.account_admin
  database    = snowflake_database.apache_flink.name
  schema      = snowflake_schema.apache_flink_schema.name
  name        = upper("flyer_stats")
  file_format = "TYPE = 'PARQUET'"
  location    = "@${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}.${snowflake_stage.flyer_stats.name}"
  auto_refresh = true

  column {
    as   = "(value:email_address::string)"
    name = "email_address"
    type = "VARCHAR"
  }

  column {
    as   = "(value:total_flight_duration::bigint)"
    name = "total_flight_duration"
    type = "BIGINT"
  }

  column {
    as   = "(value:number_of_flights::bigint)"
    name = "number_of_flights"
    type = "BIGINT"
  }

  depends_on = [ 
    snowflake_stage.flyer_stats
  ]
}
