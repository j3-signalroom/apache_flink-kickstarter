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
    snowflake_external_volume.external_volume 
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

# Snowflake Terraform Provider 2.9.0 does not support the creation of an iceberg table
resource "snowflake_execute" "skyone_airline_iceberg_table" {
  provider = snowflake
  depends_on = [ 
    snowflake_external_volume.external_volume,
    snowflake_execute.catalog_integration,
    aws_iam_role_policy_attachment.snowflake_s3_glue_policy_attachment,
    aws_iam_role.update_snowflake_s3_glue_role
  ]

  execute = <<EOT
    CREATE ICEBERG TABLE ${local.database_name}.${local.schema_name}.SKYONE_AIRLINE (
      email_address STRING,
      departure_time STRING,
      departure_airport_code STRING,
      arrival_time STRING,
      arrival_airport_code STRING,
      flight_duration BIGINT,
      flight_number STRING,
      confirmation_code STRING,
      ticket_price DECIMAL(10, 2),
      booking_agency_email STRING
    )
      EXTERNAL_VOLUME = '${local.volume_name}'
      CATALOG = '${local.catalog_integration_name}'
      CATALOG_TABLE_NAME = 'SKYONE_AIRLINE';
    EOT

  revert = <<EOT
    DROP ICEBERG TABLE ${local.database_name}.${local.schema_name}.SKYONE_AIRLINE;
  EOT

  query = <<EOT
    DESCRIBE ICEBERG TABLE ${local.database_name}.${local.schema_name}.SKYONE_AIRLINE;
  EOT
}

# Snowflake Terraform Provider 2.9.0 does not support the creation of an iceberg table
resource "snowflake_execute" "sunset_airline_iceberg_table" {
  provider = snowflake
  depends_on = [ 
    snowflake_external_volume.external_volume,
    snowflake_execute.catalog_integration,
    aws_iam_role_policy_attachment.snowflake_s3_glue_policy_attachment,
    aws_iam_role.update_snowflake_s3_glue_role
  ]

  execute = <<EOT
    CREATE ICEBERG TABLE ${local.database_name}.${local.schema_name}.SUNSET_AIRLINE (
      email_address STRING,
      departure_time STRING,
      departure_airport_code STRING,
      arrival_time STRING,
      arrival_airport_code STRING,
      flight_duration BIGINT,
      flight_number STRING,
      confirmation_code STRING,
      ticket_price DECIMAL(10, 2),
      booking_agency_email STRING
    )
      EXTERNAL_VOLUME = '${local.volume_name}'
      CATALOG = '${local.catalog_integration_name}'
      CATALOG_TABLE_NAME = 'SUNSET_AIRLINE';
    EOT

  revert = <<EOT
    DROP ICEBERG TABLE ${local.database_name}.${local.schema_name}.SUNSET_AIRLINE;
  EOT

  query = <<EOT
    DESCRIBE ICEBERG TABLE ${local.database_name}.${local.schema_name}.SUNSET_AIRLINE;
  EOT
}

# Snowflake Terraform Provider 2.9.0 does not support the creation of an iceberg table
resource "snowflake_execute" "flight_iceberg_table" {
  provider = snowflake
  depends_on = [ 
    snowflake_external_volume.external_volume,
    snowflake_execute.catalog_integration,
    aws_iam_role_policy_attachment.snowflake_s3_glue_policy_attachment,
    aws_iam_role.update_snowflake_s3_glue_role
  ]

  execute = <<EOT
    CREATE ICEBERG TABLE ${local.database_name}.${local.schema_name}.FLIGHT_AIRLINE (
      email_address STRING,
      departure_time STRING,
      departure_airport_code STRING,
      arrival_time STRING,
      arrival_airport_code STRING,
      flight_duration BIGINT,
      flight_number STRING,
      airline STRING
    )
      EXTERNAL_VOLUME = '${local.volume_name}'
      CATALOG = '${local.catalog_integration_name}'
      CATALOG_TABLE_NAME = 'FLIGHT_AIRLINE';
    EOT

  revert = <<EOT
    DROP ICEBERG TABLE ${local.database_name}.${local.schema_name}.FLIGHT_AIRLINE;
  EOT

  query = <<EOT
    DESCRIBE ICEBERG TABLE ${local.database_name}.${local.schema_name}.FLIGHT_AIRLINE;
  EOT
}

# Snowflake Terraform Provider 2.9.0 does not support the creation of an iceberg table
resource "snowflake_execute" "flyer_stats_iceberg_table" {
  provider = snowflake
  depends_on = [ 
    snowflake_external_volume.external_volume,
    snowflake_execute.catalog_integration,
    aws_iam_role_policy_attachment.snowflake_s3_glue_policy_attachment,
    aws_iam_role.update_snowflake_s3_glue_role
  ]

  execute = <<EOT
    CREATE ICEBERG TABLE ${local.database_name}.${local.schema_name}.FLYER_STATS (
      email_address STRING,
      total_flight_duration BIGINT,
      number_of_flights BIGINT
    )
      EXTERNAL_VOLUME = '${local.volume_name}'
      CATALOG = '${local.catalog_integration_name}'
      CATALOG_TABLE_NAME = 'FLYER_STATS';
    EOT

  revert = <<EOT
    DROP ICEBERG TABLE ${local.database_name}.${local.schema_name}.FLYER_STATS;
  EOT

  query = <<EOT
    DESCRIBE ICEBERG TABLE ${local.database_name}.${local.schema_name}.FLYER_STATS;
  EOT
}
