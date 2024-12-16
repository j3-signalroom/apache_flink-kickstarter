provider "snowflake" {
  role  = "SYSADMIN"

  # Snowflake Terraform Provider 1.0.0 requires the `organization_name` and 
  # `account_name` to be set, whereas the previous versions did not require
  # this.  That is why we are setting these values here.  Plus, `account` as
  # been deprecated in favor of `account_name`.
  organization_name = "${split("-", jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"])[0]}"
  account_name      = "${split("-", jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"])[1]}"
  user              = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["admin_user"]
  authenticator     = "SNOWFLAKE_JWT"
  private_key       = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["active_rsa_public_key_number"] == 1 ? data.aws_secretsmanager_secret_version.admin_private_key_1.secret_string : data.aws_secretsmanager_secret_version.admin_private_key_2.secret_string
}

resource "snowflake_warehouse" "apache_flink" {
  name           = upper(local.secrets_insert)
  warehouse_size = "xsmall"
  auto_suspend   = 60
}

resource "snowflake_database" "apache_flink" {
  name = upper(local.secrets_insert)
}

resource "snowflake_schema" "apache_flink_schema" {
  name       = upper(local.secrets_insert)
  database   = snowflake_database.apache_flink.name

  depends_on = [
    snowflake_database.apache_flink
  ]
}

resource "snowflake_storage_integration" "aws_s3_integration" {
  provider                  = snowflake.account_admin
  name                      = "AWS_S3_STORAGE_INTEGRATION"
  storage_allowed_locations = [
    "s3://flink-kickstarter/warehouse/"
  ]
  storage_provider          = "S3"
  storage_aws_object_acl    = "bucket-owner-full-control"
  storage_aws_role_arn      = local.snowflake_aws_role_arn
  enabled                   = true
  type                      = "EXTERNAL_STAGE"
}

resource "snowflake_stage" "skyone_airline" {
  name                = upper("skyone_airline_stage")
  url                 = "s3://flink-kickstarter/warehouse/airlines.db/skyone_airline/data/"
  database            = snowflake_database.apache_flink.name
  schema              = snowflake_schema.apache_flink_schema.name
  storage_integration = snowflake_storage_integration.aws_s3_integration.name
  provider            = snowflake.account_admin

  depends_on = [ 
    snowflake_storage_integration.aws_s3_integration 
  ]
}

resource "snowflake_external_table" "skyone_airline" {
  provider    = snowflake.account_admin
  database    = snowflake_database.apache_flink.name
  schema      = snowflake_schema.apache_flink_schema.name
  name        = upper("skyone_airline")
  file_format = "TYPE = 'PARQUET'"
  location    = "@${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}.${snowflake_stage.skyone_airline.name}"

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

resource "snowflake_stage" "sunset_airline" {
  name                = upper("sunset_airline_stage")
  url                 = "s3://flink-kickstarter/warehouse/airlines.db/sunset_airline/data/"
  database            = snowflake_database.apache_flink.name
  schema              = snowflake_schema.apache_flink_schema.name
  storage_integration = snowflake_storage_integration.aws_s3_integration.name
  provider            = snowflake.account_admin

  depends_on = [ 
    snowflake_storage_integration.aws_s3_integration
  ]
}

resource "snowflake_external_table" "sunset_airline" {
  provider    = snowflake.account_admin
  database    = snowflake_database.apache_flink.name
  schema      = snowflake_schema.apache_flink_schema.name
  name        = upper("sunset_airline")
  file_format = "TYPE = 'PARQUET'"
  location    = "@${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}.${snowflake_stage.sunset_airline.name}"

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

resource "snowflake_stage" "flight" {
  name                = upper("flight_stage")
  url                 = "s3://flink-kickstarter/warehouse/airlines.db/flight/data/"
  database            = snowflake_database.apache_flink.name
  schema              = snowflake_schema.apache_flink_schema.name
  storage_integration = snowflake_storage_integration.aws_s3_integration.name
  provider            = snowflake.account_admin

  depends_on = [ 
    snowflake_storage_integration.aws_s3_integration 
  ]
}

resource "snowflake_external_table" "flight" {
  provider    = snowflake.account_admin
  database    = snowflake_database.apache_flink.name
  schema      = snowflake_schema.apache_flink_schema.name
  name        = upper("flight")
  file_format = "TYPE = 'PARQUET'"
  location    = "@${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}.${snowflake_stage.flight.name}"

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
  name                = upper("flight_stage")
  url                 = "s3://flink-kickstarter/warehouse/airlines.db/flyer_stats/data/"
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
