# Apache Iceberg in Action with Apache Flink using Java
Data engineering is all about transforming raw data into useful, accessible data products in the era Data Mesh platform building. At the heart of the signalRoom GenAI Data Mesh platform, we do this by producing data products packaged in Apache Iceberg tables.  In this article, I'll take you through the process of using Apache Iceberg as a sink for your Apache Flink application using Java. This is a natural follow-up to my previous short piece, [Apache Flink + Apache Iceberg + AWS Glue: Get Your JAR Versions Right!](https://thej3.com/apache-flink-apache-iceberg-aws-glue-get-your-jar-versions-right-805041abef11) where I tackled getting the right combination of JARs in place.

Today, I'll walk you through step by step how you can seamlessly write data from a Flink application into Apache Iceberg tables, ensuring reliability, performance, and future-proof data storage. We will do this using the [Apache Flink Kickstarter Data Generator Flink app](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/DataGeneratorApp.java) I built awhile ago.

![screenshot-datageneratorapp](images/screenshot-datageneratorapp.png)

## What is Apache Iceberg?
But, before I dive into the code, you might me asking yourself what is Apache Iceberg and why it has gotten so popular over the years?  (For those who already know the answers to these questions please move on to What is AWS Glue section.)

Apache Iceberg was created in 2017 by Netflix's Ryan Blue and Daniel Weeks. It is an open table format created to resolve the deficiencies of working with data lakes, especially those on distributed storage systems like Amazon S3, Google Cloud Storage, and Azure Blob Storage. Here is a list of those problems that the Apache Iceberg open table format addresses resolve:

Problem|Challenge|Impact|Solution
-|-|-|-
**Lack of Consistency and ACID Guarantees**|Distributed storage systems are typically designed for object storage, not traditional database operations. This leads to issues with consistency, especially during concurrent read and write operations.|Without ACID (Atomicity, Consistency, Isolation, Durability) guarantees, operations like updates, deletes, and inserts can become error-prone, leading to inconsistent data views across different sessions or processes.|Apache Iceberg provides ACID compliance, ensuring reliable data consistency on distributed storage systems.
**Bloated Metatdata Files and Slow Query Performance**|As datasets grow in size, so does the metadata (file paths, schema, partitions) associated with them. Efficiently querying large volumes of metadata can become slow and inefficient.|Simple operations like listing files in a directory can become time-consuming, affecting the performance of queries and applications.|Apache Iceberg organizes data into partitions and adds metadata layers, reducing the need to scan the entire dataset and optimizing query performance. This approach allows for filtering data at the metadata level, which avoids loading unnecessary files.
**Lack of Schema Evolution and Data Mutability**|Analytic datasets often require schema changes (e.g., adding or renaming columns) as business requirements evolve. Distributed storage formats typically lack built-in support for handling schema changes efficiently.|Without schema evolution support, datasets require complex data transformations or complete rewrites, which can be slow and resource-intensive.|Apache Iceberg allows schema changes without reprocessing the entire dataset, making it easy to add new fields or alter existing ones over time.
**Inefficient Partitioning and Data Skipping**|Distributed storage systems don't natively support data partitioning, which is crucial for optimizing queries on large datasets.|Lack of partitioning increases query latency because the system has to scan more data than necessary.|Apache Iceberg allows hidden partitioning and metadata-based pruning, ensuring queries only read the required partitions, reducing scan times and improving performance.
**Lack of Data Versioning and Time Travel**|Many analytic workflows need to access previous data versions for tasks like auditing, debugging, or historical analysis. Distributed storage doesn’t offer built-in support for versioning.|Maintaining multiple versions of the same dataset becomes cumbersome, especially without efficient version control, and can lead to excessive storage costs.|Apache Iceberg offer time travel, allowing users to access snapshots of data at different points in time, providing easy access to historical data.
**Can't Do Concurrent Read and Write Operations**|Large analytic workloads often involve multiple processes reading from and writing to the same data simultaneously. Distributed storage systems do not inherently support these concurrent operations smoothly.|Without proper handling, this can lead to data corruption or version conflicts, especially during high-throughput operations.|Apache Iceberg’s transactional model enables concurrent operations safely by managing snapshots and transactions, ensuring data integrity and consistency.
**Too Many Small Files**|Distributed storage systems can accumulate many small files over time due to frequent appends or updates.|Small files lead to inefficient I/O operations and high metadata costs, degrading query performance and increasing storage costs.|Apache Iceberg handles file compaction as part of data maintenance routines, merging small files into larger ones to optimize storage and access efficiency.

By addressing these challenges, Apache Iceberg table format enable scalable, high-performance, easy-to-use and lower cost data lakehouse solutions the succesor to data lakes, which combine the best of the data warehouses and data lakes that leverage distributed storage for both analytic and streaming workloads.

### Apache Iceberg Integration Options
Apache Iceberg integrates with a variety of data processing and query systems, making it a versatile choice for modern data architectures. Here’s a breakdown of how Iceberg works with other systems and tools, and the methods it uses to streamline integration:

Feature|Details
-|-
**Interfacing with Big Data Processing Engines**|Flink, Hive, Presto, Snowflake, Spark, and Trino
**Supporting Multiple Storage Backends**|Iceberg is designed to work seamlessly with a variety of cloud-based and on-premises storage solutions, including Amazon S3, Google Cloud Storage, and Azure Blob Storage. It also supports Hadoop Distributed File System (HDFS), enabling it to operate in both cloud and hybrid environments.  The decoupling of storage and compute layers in Iceberg’s design allows users to scale compute resources independently, accommodating workloads of any size.
**Transaction Management and ACID Compliance**|Iceberg’s table format includes built-in support for ACID transactions, which ensures consistency even in environments with high concurrency. This makes it possible to execute transactions on distributed storage without risking data corruption or inconsistency.  To implement these ACID features, Iceberg uses a snapshot-based architecture. Every transaction (insert, update, delete) creates a new snapshot, and only one snapshot is considered active at a time. This architecture allows systems to “roll back” to previous snapshots, enabling time travel and easy recovery from data corruption or operational errors.
**Catalog and Metadata Management**|**Hive Metastore**: Integrates with the Hive Metastore to store metadata.  **AWS Glue**: Supports integration with AWS Glue, allowing seamless operation within AWS ecosystems.  **Custom Catalogs**: Iceberg provides a default implementation (the Hadoop Catalog) for those who don’t use Hive or AWS Glue, enabling metadata storage on local filesystems or other storage options.
**Data Versioning and Time Travel**|Each operation in Iceberg (insert, delete, update) creates a new version of the dataset, known as a snapshot. Users can query specific snapshots by timestamp or version ID, allowing them to view data as it was at any given point.  This versioning is particularly beneficial for systems requiring auditing, debugging, or historical analytics, as it enables seamless integration with analytic engines and tools that support SQL-based time-travel queries.
**Schema Evolution**|Iceberg allows users to evolve schemas without needing a full dataset rewrite. This means that users can add, rename, or delete columns dynamically.  This schema evolution is applied directly within processing engines like Spark and Flink. Iceberg tracks column changes using unique identifiers, so schema evolution won’t disrupt existing queries, making it easier to integrate into systems that handle dynamic or frequently evolving data.
**Hidden Partitioning and Predicate Pushdown**|Unlike traditional partitioning methods, Iceberg allows hidden partitioning, which automatically manages partitions behind the scenes. This reduces the complexity of working with partitioned data and improves integration with query engines by automatically filtering partitions at the metadata level. Iceberg also uses predicate pushdown, which allows query engines like Spark and Trino to apply filters directly at the metadata level. This avoids scanning unnecessary files, improving query performance and efficiency.
**Batch and Stream Processing**|Iceberg is designed to work with both batch and streaming data seamlessly. With systems like Spark Streaming and Flink, it can be integrated for real-time data processing, with the ability to read and write streams of data continuously.  Iceberg supports incremental reads and writes, which makes it well-suited for stream processing frameworks. For example, Flink and Spark’s structured streaming can query only the changes (i.e., new snapshots), which is efficient and resource-saving.
**REST API and Compatibility with External Systems**|Iceberg includes a REST API for catalog and metadata operations, which enables integrations with external data platforms, monitoring tools, and custom applications.  This API makes Iceberg accessible to a broader range of applications and platforms beyond the typical SQL-based query engines, fostering cross-functional integration and simplifying monitoring and management tasks.

By integrating Iceberg into these systems, enterprises can leverage its transactional consistency, schema flexibility, and high-performance query capabilities on distributed storage systems. This setup makes Iceberg a key component in building modern, scalable data lakehouse architectures that support both batch and streaming analytics.

#### Quick Peek into the Apache Iceberg Metadata Anatomy
Apache Iceberg Table is broken into three layers:
1. Catalog layer - Anyone reading from a table (let alone tens, hundreds, or thousands of tables) needs to know where to go first; somewhere they can go to find out where to read/write data for a given table. The first step for anyone looking to interact with the table is to find the location of the metadata file that is the current metadata pointer.  This central place where you go to find the current location of the current metadata pointer is the Iceberg catalog. The primary requirement for an Iceberg catalog is that it must support atomic operations for updating the current metadata pointer. This
support for atomic operations is required so that all readers and writers see the same state of the table at a given point in time.
2. Metadata layer - The metadata layer is an integral part of an Iceberg table’s architecture and contains all the metadata files for an Iceberg table. It’s a tree structure that tracks the datafiles and metadata about them as well as the operations that resulted in their creation.
3. Data layer - The data layer of an Apache Iceberg table is what stores the actual data of the table and is primarily made up of the datafiles themselves, although delete files are also included.

![apache-iceberg-table-structure](images/apache-iceberg-table-structure.png)

#### The True Power of Apache Iceberg
The true power of Apache Iceberg is that it allows for the true separation of storage from compute.  What this means is we are **NO LONGER LOCKED IN** to a single data vendor's compute engine!  We store the data independently of the compute engine in our distributed storage system (e.g., Amazon S3, Google Cloud Storage, and Azure Blob Storage), and then we connect to the compute engine that best fits our use case for whatever situation we are using our data in!

## The Apache Iceberg Catalog

### What is AWS Glue?

### Using Terraform to set up AWS Glue
```hcl
resource "aws_s3_bucket" "iceberg_bucket" {
  # Ensure the bucket name adheres to the S3 bucket naming conventions
  bucket = <BUCKET-NAME>
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


## Resources
Tomer Shiran, Jason Hughes & Alex Merced. [Apache Iceberg -- The Definitive Guide](https://www.dremio.com/wp-content/uploads/2023/02/apache-iceberg-TDG_ER1.pdf).  O'Reilly, 2024.
