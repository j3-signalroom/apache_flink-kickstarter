# Apache Iceberg in Action with Apache Flink using Java
Data engineering is all about transforming raw data into useful, accessible data products in the era Data Mesh platform building. At the heart of the signalRoom GenAI Data Mesh platform, we do this by producing data products packaged in Apache Iceberg tables.  In this article, I'll take you through the process of using Apache Iceberg as a sink for your Apache Flink application using Java. This is a natural follow-up to my previous short piece, [Apache Flink + Apache Iceberg + AWS Glue: Get Your JAR Versions Right!](https://thej3.com/apache-flink-apache-iceberg-aws-glue-get-your-jar-versions-right-805041abef11) where I tackled getting the right combination of JARs in place.

Today, I'll walk you through step by step how you can seamlessly write data from a Flink application into Apache Iceberg tables, ensuring reliability, performance, and future-proof data storage. We will do this using the [Apache Flink Kickstarter Data Generator Flink app](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/DataGeneratorApp.java) I built awhile ago.

![screenshot-datageneratorapp](images/screenshot-datageneratorapp.png)

## What is Apache Iceberg?  Why is it so groundbreadking?
But, before I dive into the code, you might me asking yourself what is Apache Iceberg and why it has gotten so popular over the yearst?  (For those who already know the answers to these questions please move on to What is AWS Glue section.)  Well these are all good questions!  Let's first start with what is Apache Iceberg.  

### What is Apache Iceberg?
Apache Iceberg is an open table format (method of structuring dataset files in a way to present them as a unified "table") for analytic datasets that addresses many of the challenges in working with data lakes, especially those on distributed storage like Amazon S3, Google Cloud Storage, and Azure Blob Storage. 

> _A little history:  Apache Iceberg was created in 2017 by Netflix's Ryan Blue and Daniel Weeks.  Iceberg helps manage data over time by offering key features that improve performance, consistency, and manageability in high-volume data environments._


Apache Iceberg Table is broken into three layers:
1. Catalog layer
2. Metadata layer
3. Data layer - The data layer of an Apache Iceberg table is what stores the actual data of the table and is primarily made up of the datafiles themselves, although delete files are also included.

![apache-iceberg-table-structure](images/apache-iceberg-table-structure.png)

Apache Iceberg is built on a modular architecture that provides a framework for managing datasets in a distributed environment. Its components work together to manage data storage, organization, and access efficiently, especially on cloud-based storage systems. Here’s a breakdown of its primary components:

### 1. **Table Format**
   - **Manifest Files**: Store information about each file in the table. This includes metadata like file paths, row counts, and column statistics, which helps optimize data pruning during queries.
   - **Manifest Lists**: Aggregate manifests into a single place, representing a snapshot of the dataset. These lists make it easy to roll back to previous versions or view the history of changes.
   - **Data Files**: The actual data files stored in formats like Parquet, Avro, or ORC. These are partitioned and organized based on the table’s partitioning strategy, with schema evolution features allowing changes without rewriting data.

### 2. **Metadata Layer**
   - **Snapshots**: Represent a point-in-time view of the data, allowing features like time travel, which enables users to query previous versions of the table.
   - **Schema and Partition Evolution**: Enables adding, deleting, and updating columns and partition strategies without rewriting data. This layer simplifies schema updates and manages changes efficiently.
   - **Table Properties and Configuration**: Stores configuration and properties at the table level, defining things like file formats, snapshot expiration, and more.

### 3. **Transaction Management**
   - **ACID Compliance**: Iceberg provides ACID transactions to handle data integrity and consistency. This is critical for handling updates, deletes, and upserts.
   - **Locking and Concurrency Control**: Ensures consistent reads and writes, even in highly concurrent environments, through isolation mechanisms that prevent conflicts between operations.

### 4. **Data Pruning and Filtering**
   - **Predicate Pushdown**: Filters data at the metadata level to avoid unnecessary scanning, speeding up query execution by focusing only on relevant files.
   - **Partitioning and Partition Pruning**: Iceberg supports dynamic partitioning, which allows efficient partition pruning based on queries. It supports hidden partitioning, so users don’t have to worry about specific partition columns.

### 5. **Integration with Processing Engines**
   - **APIs and Catalogs**: Iceberg provides a unified API layer and catalog services to integrate with popular data processing engines like Apache Flink, Snowflake, Apache Spark (AWS Glue), Trino, Hive, and Presto. These catalogs manage table metadata and facilitate operations across multiple processing engines.  _With catalogs, users don’t need separate metadata management for each engine; any supported engine can use the catalog to access the same table definitions and metadata, ensuring consistency across platforms._
   - **Vectorized Read and Write**: Optimized I/O operations for compatible formats (like Parquet and ORC) enable faster processing and are especially useful for complex analytical queries.

### 6. **Time Travel and Versioning**
   - **Historical Data Access**: The snapshot-based versioning allows users to access previous data states, which is useful for debugging, auditing, or analyzing past trends.
   - **Rollback and Data Auditing**: Users can roll back to previous snapshots, making it easy to undo changes or access historical data without additional data management overhead.

Apache Iceberg’s layered design, with manifest and snapshot files and support for partition and schema evolution, makes it highly adaptable for use cases involving large, evolving datasets where efficient access and consistency are essential.

### Why is it so groundbreadking?

## The Apache Iceberg Catalog

### What is AWS Glue?

### Using Terraform to set up AWS Glue
```hcl
resource "aws_s3_bucket" "iceberg_bucket" {
  # Ensure the bucket name adheres to the S3 bucket naming conventions
  bucket = replace(local.secrets_insert, "_", "-")
}

resource "aws_s3_object" "warehouse" {
  bucket = aws_s3_bucket.iceberg_bucket.bucket
  key    = "warehouse/"
}

resource "aws_iam_role" "glue_role" {
  name = "glue_service_role"

  assume_role_policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "glue.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_policy" "glue_s3_access_policy" {
  name = "GlueS3AccessPolicy"

  policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "s3:GetObject",
          "s3:PutObject",
          "s3:ListBucket"
        ],
        "Resource": [
          aws_s3_bucket.iceberg_bucket.arn,
          "${aws_s3_bucket.iceberg_bucket.arn}/*"
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "glue_policy_attachment" {
  role       = aws_iam_role.glue_role.name
  policy_arn = aws_iam_policy.glue_s3_access_policy.arn
}

resource "aws_glue_catalog_database" "iceberg_db" {
  name = "iceberg_database"
}
```
