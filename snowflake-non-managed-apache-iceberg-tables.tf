resource "snowflake_storage_integration" "aws_s3_integration" {
  name                      = "AWS_S3_STORAGE_INTEGRATION"
  storage_allowed_locations = ["s3://flink-kickstarter/warehouse/airlines.db/"]
  storage_provider          = "S3"
  storage_aws_object_acl    = "bucket-owner-full-control"
  storage_aws_role_arn      = aws_iam_policy.glue_s3_access_policy.arn
  enabled                   = true
}

resource "snowflake_file_format" "parquet_format" {
  name        = "APACHE_ICEBERG_TABLE_PARQUET_FORMAT"
  database    = "flink_kickstarter"
  schema      = "flink_kickstarter"
  format_type = "PARQUET"
  comment     = "Parquet file format"
}

resource "snowflake_stage" "skyone_airline_stage" {
  name                = "SKYONE_AIRLINE_STAGE"
  url                 = "s3://flink-kickstarter/warehouse/airlines.db/skyone_airline/"
  database            = "flink_kickstarter"
  schema              = "flink_kickstarter"
  storage_integration = snowflake_storage_integration.aws_s3_integration.name
}

resource "snowflake_external_table" "skyone_airline_table" {
  database    = "flink_kickstarter"
  schema      = "flink_kickstarter"
  name        = "SKYONE_AIRLINE_STAGE_EXTERNAL_TABLE"
  file_format = snowflake_file_format.parquet_format.name
  location    = snowflake_stage.skyone_airline_stage.name 

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
}

resource "snowflake_stage" "sunset_airline_stage" {
  name                = "SUNSET_AIRLINE_STAGE"
  url                 = "s3://flink-kickstarter/warehouse/airlines.db/sunset_airline/"
  database            = "flink_kickstarter"
  schema              = "flink_kickstarter"
  storage_integration = snowflake_storage_integration.aws_s3_integration.name
}

resource "snowflake_external_table" "sunset_airline_table" {
  database    = "flink_kickstarter"
  schema      = "flink_kickstarter"
  name        = "SUNSET_AIRLINE_STAGE_EXTERNAL_TABLE"
  file_format = snowflake_file_format.parquet_format.name
  location    = snowflake_stage.sunset_airline_stage.name 

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
}
