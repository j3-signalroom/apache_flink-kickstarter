provider "snowflake" {
  role  = "SYSADMIN"

  # The most recently version of Snowflake Terraform Provider requires the 
  # `organization_name` and `account_name` to be set, whereas the previous 
  # versions did not require this.  That is why we are setting these values
  # here.  Plus, `account` as been deprecated in favor of `account_name`.
  organization_name = "${split("-", jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"])[0]}"
  account_name      = "${split("-", jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["account"])[1]}"
  user              = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["admin_user"]
  authenticator     = "JWT"
  private_key       = jsondecode(data.aws_secretsmanager_secret_version.admin_public_keys.secret_string)["active_rsa_public_key_number"] == 1 ? data.aws_secretsmanager_secret_version.admin_private_key_1.secret_string : data.aws_secretsmanager_secret_version.admin_private_key_2.secret_string
}

resource "snowflake_warehouse" "apache_flink" {
  name           = local.secrets_insert
  warehouse_size = "xsmall"
  auto_suspend   = 60
}

resource "snowflake_database" "apache_flink" {
  name = local.secrets_insert
}

resource "snowflake_schema" "schema" {
  database   = snowflake_database.apache_flink.name
  name       = local.secrets_insert
}

resource "snowflake_storage_integration" "aws_s3_integration" {
  provider                  = snowflake.account_admin
  name                      = "AWS_S3_STORAGE_INTEGRATION"
  storage_allowed_locations = ["s3://flink-kickstarter/warehouse/airlines.db/"]
  storage_provider          = "S3"
  storage_aws_object_acl    = "bucket-owner-full-control"
  storage_aws_role_arn      = aws_iam_role.snowflake_role.arn
  enabled                   = true
  type                      = "EXTERNAL_STAGE"

  depends_on = [ 
    aws_iam_role.snowflake_role
  ]
}

resource "snowflake_file_format" "parquet_format" {
  name        = "APACHE_ICEBERG_TABLE_PARQUET_FORMAT"
  database    = "flink_kickstarter"
  schema      = "flink_kickstarter"
  format_type = "PARQUET"
  comment     = "Parquet file format"
}

resource "snowflake_stage" "skyone_airline_stage" {
  name                = "skyone_airline_stage"
  url                 = "s3://flink-kickstarter/warehouse/airlines.db/skyone_airline/"
  database            = "flink_kickstarter"
  schema              = "flink_kickstarter"
  storage_integration = snowflake_storage_integration.aws_s3_integration.name
  provider            = snowflake.account_admin

  depends_on = [ snowflake_storage_integration.aws_s3_integration ]
}

resource "snowflake_external_table" "skyone_airline_table" {
  database    = "flink_kickstarter"
  schema      = "flink_kickstarter"
  name        = "skyone_airline_stage_external_table"
  file_format = snowflake_file_format.parquet_format.name
  location    = "@${snowflake_database.apache_flink.name}.${snowflake_schema.schema.name}.${snowflake_stage.skyone_airline_stage.name}/"

  column {
    as   = "EMAIL_ADDRESS"
    name = "EMAIL_ADDRESS"
    type = "STRING"
  }

  column {
    as   = "DEPATURE_TIME"
    name = "DEPATURE_TIME"
    type = "STRING"
  }

  column {
    as   = "DEPATURE_AIRPORT_CODE"
    name = "DEPATURE_AIRPORT_CODE"
    type = "STRING"
  }

  column {
    as   = "ARRIVAL_TIME"
    name = "ARRIVAL_TIME"
    type = "STRING"
  }

  column {
    as   = "ARRIVAL_AIRPORT_CODE"
    name = "ARRIVAL_AIRPORT_CODE"
    type = "STRING"
  }

  column {
    as   = "FLIGHT_DURATION"
    name = "FLIGHT_DURATION"
    type = "BIGINT"
  }

  column {
    as   = "FLIGHT_NUMBER"
    name = "FLIGHT_NUMBER"
    type = "STRING"
  }

  column {
    as   = "CONFIRMATION_NUMBER"
    name = "CONFIRMATION_NUMBER"
    type = "STRING"
  }

  column {
    as   = "TICKET_PRICE"
    name = "TICKET_PRICE"
    type = "NUMBER"
  }

  column {
    as   = "BOOKING_AGENCY_EMAIL"
    name = "BOOKING_AGENCY_EMAIL"
    type = "STRING"
  }

  depends_on = [ snowflake_stage.skyone_airline_stage ]
}

resource "snowflake_stage" "sunset_airline_stage" {
  name                = "sunset_airline_stage"
  url                 = "s3://flink-kickstarter/warehouse/airlines.db/sunset_airline/"
  database            = "flink_kickstarter"
  schema              = "flink_kickstarter"
  storage_integration = snowflake_storage_integration.aws_s3_integration.name
  provider            = snowflake.account_admin

  depends_on = [ snowflake_storage_integration.aws_s3_integration ]
}

resource "snowflake_external_table" "sunset_airline_table" {
  database    = "flink_kickstarter"
  schema      = "flink_kickstarter"
  name        = "sunset_airline_stage_external_table"
  file_format = snowflake_file_format.parquet_format.name
  location    = "@${snowflake_database.apache_flink.name}.${snowflake_schema.schema.name}.${snowflake_stage.sunset_airline_stage.name}/"

  column {
    as   = "EMAIL_ADDRESS"
    name = "EMAIL_ADDRESS"
    type = "STRING"
  }

  column {
    as   = "DEPATURE_TIME"
    name = "DEPATURE_TIME"
    type = "STRING"
  }

  column {
    as   = "DEPATURE_AIRPORT_CODE"
    name = "DEPATURE_AIRPORT_CODE"
    type = "STRING"
  }

  column {
    as   = "ARRIVAL_TIME"
    name = "ARRIVAL_TIME"
    type = "STRING"
  }

  column {
    as   = "ARRIVAL_AIRPORT_CODE"
    name = "ARRIVAL_AIRPORT_CODE"
    type = "STRING"
  }

  column {
    as   = "FLIGHT_DURATION"
    name = "FLIGHT_DURATION"
    type = "BIGINT"
  }

  column {
    as   = "FLIGHT_NUMBER"
    name = "FLIGHT_NUMBER"
    type = "STRING"
  }

  column {
    as   = "CONFIRMATION_NUMBER"
    name = "CONFIRMATION_NUMBER"
    type = "STRING"
  }

  column {
    as   = "TICKET_PRICE"
    name = "TICKET_PRICE"
    type = "NUMBER"
  }

  column {
    as   = "BOOKING_AGENCY_EMAIL"
    name = "BOOKING_AGENCY_EMAIL"
    type = "STRING"
  }

  depends_on = [ snowflake_stage.sunset_airline_stage ]
}
