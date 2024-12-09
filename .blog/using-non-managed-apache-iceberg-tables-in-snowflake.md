# Using Non-Managed Apache Iceberg Tables in Snowflake
Apache Iceberg is a table format that is designed to be used with big data processing engines like Apache Spark and Presto. It provides a way to manage large datasets in a way that is efficient and scalable. Snowflake is a cloud-based data warehousing platform that is designed to be fast, flexible, and easy to use. In this article, I will show you how to connect Apache Iceberg tables to a Snowflake schema.

Snowflake Iceberg Tables support Iceberg in two ways: an Internal Catalog (Snowflake-managed catalog) or an externally managed catalog (AWS Glue or Objectstore).

Iceberg Tables: Snowflake-managed catalog
A Snowflake-managed catalog is nearly identical performance as a regular Snowflake table and has the following characteristics.

Snowflake reads/writes
Iceberg interoperability
Full platform support
Performance optimized


Iceberg Tables: Externally managed catalog
Externally managed catalogs like AWS Glue or you can use Iceberg metadata files stored in object storage to create a table and have the following characteristics:

Flexible sources
Efficient onboarding
Simplified operations
Performance optimized


Icerberg Table Convertion
In practical terms, a managed Iceberg table vs non-managed is the ability to write data inside Snowflake or not, for managed you can write and for non-managed, it’s read-only.