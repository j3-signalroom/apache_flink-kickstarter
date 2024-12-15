# Using Non-Managed Apache Iceberg Tables in Snowflake
Apache Iceberg is a table format that is designed to be used with big data processing engines like Apache Spark and Presto. It provides a way to manage large datasets in a way that is efficient and scalable. Snowflake is a cloud-based data warehousing platform that is designed to be fast, flexible, and easy to use. In this article, I will show you how to connect Apache Iceberg tables to a Snowflake schema.

## Snowflake Managed Apache Iceberg Tables
Snowflake supports Apache Iceberg in two ways: an Internal Catalog (Snowflake-managed catalog) or an externally managed catalog (AWS Glue or Objectstore).

### Apache Iceberg Tables: Snowflake-managed catalog
A Snowflake-managed catalog is nearly identical performance as a regular Snowflake table and has the following characteristics:

- Snowflake reads/writes
- Apache Iceberg interoperability
- Full platform support
- Performance optimized

### Apache Iceberg Tables: Externally managed catalog
Externally managed catalogs like AWS Glue or you can use Apache Iceberg metadata files stored in object storage to create a table and have the following characteristics:

- Flexible sources
- Efficient onboarding
- Simplified operations
- Performance optimized

## Non-Managed Apache Iceberg Tables in Snowflake
Non-Managed Apache Iceberg tables in Snowflake are read-only tables that are created using the Apache Iceberg metadata files stored in object storage. These tables are useful for scenarios where you want to query data that is stored in Apache Iceberg format without having to write the data into Snowflake.


### Snowflake Storage Integration to access Amazon S3
Integrations are named, first-class Snowflake objects that avoid the need for passing explicit cloud provider credentials such as secret keys or access tokens. Integration objects store an AWS identity and access management (IAM) user ID. An administrator in your organization grants the integration IAM user permissions in the AWS account.

## References
[Configuring a Snowflake storage integration to access Amazon S3](https://docs.snowflake.com/en/user-guide/data-load-s3-config-storage-integration.html)
[SYSTEM$VALIDATE_STORAGE_INTEGRATION](https://docs.snowflake.com/en/sql-reference/functions/system_validate_storage_integration)


