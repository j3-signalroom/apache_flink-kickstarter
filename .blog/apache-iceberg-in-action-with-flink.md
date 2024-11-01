# Apache Iceberg in Action with Apache Flink using Java
Data engineering transforms raw data into useful, accessible data products in the Data Mesh platform-building era. Like the signalRoom GenAI Data Mesh platform, we package our data products in Apache Iceberg tables. In this article, I'll take you through sinking your Apache Flink data into Apache Iceberg tables using Java. This is a natural follow-up to my previous short piece, [Apache Flink + Apache Iceberg + AWS Glue: Get Your JAR Versions Right!](https://thej3.com/apache-flink-apache-iceberg-aws-glue-get-your-jar-versions-right-805041abef11) where I listed out the right combination of JARs to use.

In this article, I'll walk you through how to seamlessly sink data in your Flink application to Apache Iceberg tables, ensuring reliability, performance, and future-proof data storage. We will do this using the [Apache Flink Kickstarter Data Generator Flink app](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/DataGeneratorApp.java) I built. This app generates synthetic flight data for two fictional airlines (`Sunset Air` and `SkyOne`) and streams it into Apache Kafka and Apache Iceberg. The app provides real-time and historical analytics capabilities, demonstrating the power of Apache Iceberg as a table format for large, complex analytic datasets in distributed data lakehouses.  Moreover, how AWS Glue is used as the metadata catalog for the Apache Iceberg tables.

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
1. Catalog layer - Responsible for managing the high-level registration and discovery of tables, namespaces, and integration with metadata catalogs like AWS Glue, Hive Metastore, and others. It provides a registry for storing and locating Iceberg tables.
2. Metadata layer - Manages all metadata, such as schema, partition information, snapshots, and file statistics. It’s crucial for tracking versions (snapshots), supporting schema evolution, time travel, and optimizing queries to read only relevant data files.
3. Data layer - The physical layer that stores the actual dataset files in distributed and scalable storage systems. It ensures data is efficiently organized for analytics workloads, leveraging cloud or distributed storage (S3, HDFS) and file formats (Parquet, Avro, ORC). It also handles partitioning, compaction, and ACID transactions for reliable data operations.

![apache-iceberg-table-structure](images/apache-iceberg-table-structure.png)

#### The True Power of Apache Iceberg is its Catalog
The true power of Apache Iceberg is that it allows for the true separation of storage from compute.  What this means is we are **NO LONGER LOCKED IN** to a single data vendor's compute engine!  We store the data independently of the compute engine in our distributed storage system (e.g., Amazon S3, Google Cloud Storage, and Azure Blob Storage), and then we connect to the compute engine that best fits our use case for whatever situation we are using our data in!

### In this demo we AWS Glue as our catalog for Apache Iceberg
**AWS Glue** is a fully managed extract, transform, and load (**ETL**) service offered by Amazon Web Services (**AWS**). It simplifies the process of preparing and loading data for analytics by automating data discovery, schema inference, and job scheduling. AWS Glue provides a comprehensive platform that includes:

- **AWS Glue Data Catalog**: A centralized metadata repository that stores information about data sources, schemas, and transformations.

**Apache Iceberg** is an open-source table format designed for large, complex analytic datasets in distributed data lakes. It provides capabilities like schema evolution, hidden partitioning, ACID transactions, and time travel queries. Iceberg simplifies data management and improves performance for big data analytics.

**Amazon S3** (Simple Storage Service) is AWS's scalable, high-speed, web-based cloud storage service designed for online backup and archiving of data and applications.

### **Integration of AWS Glue with Apache Iceberg and Amazon S3**

The integration of AWS Glue with Apache Iceberg and Amazon S3 enables you to build a robust, scalable, and efficient data lake solution. Here's how they work together:

#### **1. Data Storage on Amazon S3**

- **Data Files**: Apache Iceberg stores the actual dataset as data files (e.g., Parquet, ORC, or Avro) in Amazon S3 buckets. S3 provides durable and highly available storage for these files.
- **Metadata Files**: Iceberg maintains metadata files in S3 as well. These files include table snapshots, manifest lists, and manifest files that track the state and structure of the table over time.

#### **2. AWS Glue Data Catalog as the Metastore**

- **Centralized Metadata Management**: The AWS Glue Data Catalog serves as the metastore for Apache Iceberg tables. It holds metadata such as table schemas, partitioning information, and pointers to data locations in S3.
- **Schema Evolution Support**: Iceberg's ability to handle schema changes without rewriting data is supported through the Data Catalog, allowing seamless schema evolution.

#### CI/CD [using Terraform] to set up the AWS Glue infrastructure
Before we dive into the code, let's set up the AWS Glue infrastructure using Terraform.  This will allow us to have a repeatable process to set up the AWS Glue infrastructure.  Here is the Terraform code to set up the AWS Glue infrastructure:

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
This Terraform code is designed to set up the necessary infrastructure for integrating **AWS Glue**, **Amazon S3**, and **Iceberg**, specifically to store Iceberg tables in S3, manage metadata through Glue, and ensure that the appropriate IAM roles and policies are in place for permissions.

### Detailed Breakdown

#### 1. **S3 Bucket for Iceberg Data**
```hcl
resource "aws_s3_bucket" "iceberg_bucket" {
  # Ensure the bucket name adheres to the S3 bucket naming conventions
  bucket = <BUCKET-NAME>
}
```
- **`aws_s3_bucket "iceberg_bucket"`**: Creates an Amazon S3 bucket for storing the Iceberg data.
- **`bucket = <BUCKET-NAME>`**: The bucket name should be unique and adhere to the S3 naming conventions. This bucket will act as the **warehouse** for Iceberg, where all the data files (e.g., Parquet files) are stored.

#### 2. **S3 Object Placeholder for Warehouse**
```hcl
resource "aws_s3_object" "warehouse" {
  bucket = aws_s3_bucket.iceberg_bucket.bucket
  key    = "warehouse/"
}
```
- **`aws_s3_object "warehouse"`**: Creates a placeholder object in the S3 bucket, representing the directory named `warehouse/`.
- **`bucket = aws_s3_bucket.iceberg_bucket.bucket`**: Specifies that this object belongs to the previously created `iceberg_bucket`.
- **`key = "warehouse/"`**: Sets the key to represent a directory structure for the Iceberg warehouse.

#### 3. **IAM Role for AWS Glue**
```hcl
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
```
- **`aws_iam_role "glue_role"`**: Creates an IAM role for AWS Glue to allow it to interact with other AWS services.
- **`assume_role_policy`**:
  - Defines a **trust policy** that allows the Glue service to assume this role.
  - The principal is set to `"glue.amazonaws.com"`, allowing **AWS Glue** to use the role.

#### 4. **IAM Policy for S3 Access**
```hcl
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
```
- **`aws_iam_policy "glue_s3_access_policy"`**: Defines an IAM policy that allows **AWS Glue** to interact with the S3 bucket used for storing Iceberg data.
- **`policy`**:
  - Specifies the actions that Glue can perform on the S3 bucket.
  - **Actions**:
    - **`s3:GetObject`**: Allows Glue to read objects from the S3 bucket.
    - **`s3:PutObject`**: Allows Glue to write objects to the S3 bucket.
    - **`s3:ListBucket`**: Allows Glue to list the contents of the bucket.
  - **Resources**:
    - **`aws_s3_bucket.iceberg_bucket.arn`**: Grants permissions to the bucket itself.
    - **`${aws_s3_bucket.iceberg_bucket.arn}/*`**: Grants permissions to all objects within the bucket.

#### 5. **Attach IAM Policy to the Glue Role**
```hcl
resource "aws_iam_role_policy_attachment" "glue_policy_attachment" {
  role       = aws_iam_role.glue_role.name
  policy_arn = aws_iam_policy.glue_s3_access_policy.arn
}
```
- **`aws_iam_role_policy_attachment "glue_policy_attachment"`**: Attaches the previously created S3 access policy (`glue_s3_access_policy`) to the Glue role (`glue_role`).
- **`role`**: Specifies the name of the IAM role to which the policy will be attached.
- **`policy_arn`**: Specifies the ARN of the policy being attached to the role.

#### 6. **Glue Catalog Database for Iceberg Metadata**
```hcl
resource "aws_glue_catalog_database" "iceberg_db" {
  name = "iceberg_database"
}
```
- **`aws_glue_catalog_database "iceberg_db"`**: Creates a new **AWS Glue catalog database** named `"iceberg_database"`.
- This database will be used to store the metadata for Iceberg tables, providing schema management, partitioning information, and other table-level metadata.

### Summary
- **S3 Bucket and Warehouse Directory**:
  - Creates an S3 bucket (`iceberg_bucket`) to act as the data warehouse for Apache Iceberg.
  - Creates a placeholder object (`warehouse`) to represent the warehouse directory in the bucket.
- **Glue Role and Policy**:
  - **`glue_role`**: Creates an IAM role that allows AWS Glue to interact with S3.
  - **`glue_s3_access_policy`**: Defines the permissions needed for Glue to read/write to the S3 bucket (`GetObject`, `PutObject`, `ListBucket`).
  - The role and policy are then attached to ensure Glue has the appropriate permissions to perform ETL jobs that involve reading from and writing to the Iceberg warehouse in S3.
- **Glue Catalog Database**:
  - Creates an AWS Glue database (`iceberg_db`) to manage the metadata of Iceberg tables.
  - This database will be used by Apache Iceberg for managing the table schemas and providing easy integration with other AWS services for querying and managing datasets.

This Terraform code is an integral part of setting up an **AWS Glue** and **Iceberg** infrastructure that can be used for managing metadata, storing data files in S3, and providing permissions for Glue to manage the lifecycle of Iceberg tables. The setup is ideal for implementing data lakehouse solutions that need efficient metadata handling and seamless integration with AWS services.

This class, `DataGeneratorApp`, is a comprehensive example of a Flink application that generates synthetic flight data, streams it into both Apache Kafka and Apache Iceberg, and provides both real-time and historical analytics capabilities. Let me summarize its main functionalities and key features:

### Overview
- **Purpose**: 
  - To generate fake flight data for two fictional airlines (`Sunset Air` and `Sky One`) and stream this data into Kafka topics and Apache Iceberg tables.
  - Use Apache Flink to build a streaming pipeline with **Flink DataStream API** and **Apache Iceberg** integration using **AWS Glue**.

- **Technology Stack**:
  - **Apache Flink**: Used to define the data stream processing pipeline.
  - **Apache Kafka**: Acts as the messaging platform to publish streaming data.
  - **Apache Iceberg**: Stores data in a table format, providing efficient data access with features like partitioning and snapshotting.
  - **AWS Glue**: Used as the metadata catalog for Iceberg tables.

### Key Functionalities
This code snippet is responsible for configuring and setting up a Flink streaming pipeline, where synthetic flight data is generated for two airlines, `Sky One` and `Sunset Air`. The data is then streamed into Kafka topics for downstream processing or consumption. Let's go through each part of the code in detail:



#### 1. Retrieve Command Line Arguments
```java
String serviceAccountUser = Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER);
String awsRegion = Common.getAppArgumentValue(args, Common.ARG_AWS_REGION);
```
- **Command Line Arguments**: Retrieves the `serviceAccountUser` and `awsRegion` values from the command line arguments.
  - These values are used later for configuration, like accessing AWS services or following S3 naming conventions.

#### 2. Set Up Flink Execution Environment
```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
env.enableCheckpointing(5000);
env.getCheckpointConfig().setCheckpointTimeout(60000);
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
```
- **Execution Environment**: Creates the Flink `StreamExecutionEnvironment`, which represents the Flink job's DAG (Directed Acyclic Graph).
- **Checkpointing**:
  - **`enableCheckpointing(5000)`**: Enables checkpointing every 5 seconds to ensure fault tolerance.
  - **Checkpoint Timeout**: Sets a timeout of 60 seconds for each checkpoint (`setCheckpointTimeout(60000)`).
  - **Max Concurrent Checkpoints**: Limits concurrent checkpoints to one (`setMaxConcurrentCheckpoints(1)`), ensuring that only one checkpoint is taken at a time.

```java
EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
StreamTableEnvironment tblEnv = StreamTableEnvironment.create(env, settings);
```
- **Table Environment**: Creates a `StreamTableEnvironment` (`tblEnv`) to work with Flink's Table API, which allows for SQL-like operations and integration with other data processing systems.

#### 3. Retrieve Kafka Properties
```java
DataStream<Properties> dataStreamProducerProperties = 
    env.fromData(new Properties())
       .map(new KafkaClientPropertiesLookup(false, serviceAccountUser))
       .name("kafka_producer_properties");
Properties producerProperties = new Properties();
```
- **Kafka Properties Lookup**: Uses `KafkaClientPropertiesLookup` to fetch the Kafka properties (e.g., broker addresses, security settings) from AWS services (like AWS Secrets Manager).
- **Create Data Stream**: Creates a `DataStream<Properties>` that contains the Kafka producer properties.

```java
try {
    dataStreamProducerProperties
        .executeAndCollect()
        .forEachRemaining(typeValue -> {
            producerProperties.putAll(typeValue);
        });
} catch (final Exception e) {
    System.out.println("The Flink App stopped during the reading of the custom data source stream because of the following: " + e.getMessage());
    e.printStackTrace();
    System.exit(1);
}
```
- **Execute and Collect**: Executes the data stream and collects the Kafka properties. This step is required to ensure that the `producerProperties` are available for setting up the Kafka sinks.
- **Error Handling**: If any exception occurs during this process, the application prints the error, logs it, and exits with a non-zero status.

#### 4. Create Data Sources
**Sky One Source**:
```java
DataGeneratorSource<AirlineData> skyOneSource =
    new DataGeneratorSource<>(
        index -> DataGenerator.generateAirlineFlightData("SKY1"),
        Long.MAX_VALUE,
        RateLimiterStrategy.perSecond(1),
        Types.POJO(AirlineData.class)
    );
DataStream<AirlineData> skyOneStream = 
    env.fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");
```
- **Data Generator for Sky One**: Generates synthetic flight data (`AirlineData`) for `Sky One` airline using `DataGeneratorSource`. The generator runs indefinitely (`Long.MAX_VALUE`) and generates one record per second (`RateLimiterStrategy.perSecond(1)`).
- **Create Data Stream**: Converts the data source into a `DataStream<AirlineData>` named `skyOneStream`.

**Sunset Source**:
```java
DataGeneratorSource<AirlineData> sunsetSource =
    new DataGeneratorSource<>(
        index -> DataGenerator.generateAirlineFlightData("SUN"),
        Long.MAX_VALUE,
        RateLimiterStrategy.perSecond(1),
        Types.POJO(AirlineData.class)
    );
DataStream<AirlineData> sunsetStream = 
    env.fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");
```
- **Data Generator for Sunset Air**: Similarly, creates a data generator for `Sunset Air` airline.
- **Create Data Stream**: Converts the data source into a `DataStream<AirlineData>` named `sunsetStream`.

#### 5. Create Kafka Sinks
**Sky One Sink**:
```java
KafkaRecordSerializationSchema<AirlineData> skyOneSerializer = 
    KafkaRecordSerializationSchema.<AirlineData>builder()
        .setTopic("airline.skyone")
        .setValueSerializationSchema(new JsonSerializationSchema<>(Common::getMapper))
        .build();

KafkaSink<AirlineData> skyOneSink = 
    KafkaSink.<AirlineData>builder()
        .setKafkaProducerConfig(producerProperties)
        .setRecordSerializer(skyOneSerializer)
        .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
        .build();
```
- **Serialization Schema**: Creates a serialization schema for `Sky One` using `JsonSerializationSchema`, which converts `AirlineData` objects to JSON format.
- **Kafka Sink**:
  - Configures a Kafka sink (`KafkaSink<AirlineData>`) with the producer properties retrieved earlier.
  - Uses **`AT_LEAST_ONCE`** delivery guarantee to ensure that messages are not lost, although duplicates may be possible.

**Sunset Sink**:
```java
KafkaRecordSerializationSchema<AirlineData> sunsetSerializer = 
    KafkaRecordSerializationSchema.<AirlineData>builder()
        .setTopic("airline.sunset")
        .setValueSerializationSchema(new JsonSerializationSchema<>(Common::getMapper))
        .build();

KafkaSink<AirlineData> sunsetSink = 
    KafkaSink.<AirlineData>builder()
        .setKafkaProducerConfig(producerProperties)
        .setRecordSerializer(sunsetSerializer)
        .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
        .build();
```
- **Serialization Schema**: Similarly, creates a serializer for the `Sunset Air` data to be published to the `airline.sunset` topic.
- **Kafka Sink**: Sets up the Kafka sink for `Sunset Air` with the same configurations as `Sky One`.

#### 6. Attach Sources and Sinks to Flink's DAG
```java
skyOneStream.sinkTo(skyOneSink).name("skyone_sink");
sunsetStream.sinkTo(sunsetSink).name("sunset_sink");
```
- **Attach Sinks**: Adds the `KafkaSink` for both `Sky One` and `Sunset Air` to the Flink data streams (`skyOneStream` and `sunsetStream`).
  - Only streams with sinks attached will be executed when the `StreamExecutionEnvironment.execute()` method is called.


This setup allows for generating real-time flight data and sending it to Kafka topics, which can be further used for downstream analytics, monitoring, or storage in a data lake (e.g., Iceberg). The application is highly fault-tolerant, supports scalability, and can work seamlessly with cloud-native environments by leveraging AWS services for metadata and property management.

1. **Main Method - Entry Point**
   - The `main()` method sets up the Flink environment, creates data generators, defines data sinks, and eventually executes the streaming job.
   - The method outlines the following steps:
     1. **Setup Execution Environment**: Configure Flink for streaming jobs, including checkpointing to ensure fault tolerance.
     2. **Configure Kafka Producer**: Retrieve Kafka producer properties from AWS Secrets Manager and set up Kafka sinks for data streams.
     3. **Generate and Stream Flight Data**: Use a `DataGeneratorSource` to generate flight data for `Sky One` and `Sunset Air`.
     4. **Sink Data to Kafka**: Serialize the data in JSON format and publish it to Kafka topics.
     5. **Iceberg Catalog Configuration**: Set up the Apache Iceberg catalog using AWS Glue for metadata management.
     6. **Sink Data to Iceberg Table**: Convert the data stream to `RowData` format and write it to Iceberg tables.

2. **Flink Configuration and Checkpointing**
   - **Checkpointing**: Enabled every 5000 milliseconds (5 seconds) to ensure that the data stream processing can recover in case of failures.
   - **Checkpoint Timeout**: Set to 60 seconds, limiting the duration for completing a checkpoint.
   - **Max Concurrent Checkpoints**: Limited to 1 to prevent multiple checkpoints from overwhelming resources.

3. **Data Generation Sources**
   - **DataGeneratorSource**: A Flink connector used to generate fake flight data (`AirlineData`) for both airlines.
   - The generated data streams (`skyOneStream` and `sunsetStream`) are processed using Flink's streaming API.

4. **Kafka Sink Configuration**
   - **Kafka Serialization Schema**: Uses `JsonSerializationSchema` to serialize `AirlineData` objects into JSON before publishing them.
   - **Kafka Sink Setup**: Publishes the generated data to Kafka topics (`airline.skyone` and `airline.sunset`), with a delivery guarantee of `AT_LEAST_ONCE`.

5. **Apache Iceberg Integration**
   - **Catalog Setup**:
     - Iceberg is configured to use `AWS Glue` as the catalog and `Amazon S3` as the data warehouse location.
     - Properties like `catalog-impl` and `io-impl` are set to ensure that Glue is used for metadata and S3 is used for I/O.
   - **Database Creation**: The `airlines` database is created if it doesn’t exist.
   - **Table Creation and Data Sink**:
     - Defines the table schema (`RowType`) that includes fields like `email_address`, `departure_time`, `ticket_price`, etc.
     - Converts `DataStream<AirlineData>` to `DataStream<RowData>`, and writes the resulting stream to Iceberg tables (`skyone_airline` and `sunset_airline`) using the `FlinkSink`.

    #### 1. Setting Up Iceberg Catalog Configuration
    ```java
    String catalogName = "apache_kickstarter";
    String bucketName = serviceAccountUser.replace("_", "-");  // --- To follow S3 bucket naming convention, replace underscores with hyphens if exist in string.
    String catalogImpl = "org.apache.iceberg.aws.glue.GlueCatalog";
    String databaseName = "airlines";
    Map<String, String> catalogProperties = new HashMap<>();
    ```
    - **`catalogName`**: The name of the Iceberg catalog (`apache_kickstarter`), which will be used to reference this catalog in the Flink environment.
    - **`bucketName`**: The S3 bucket where the data will be stored. The code ensures the bucket name follows S3 naming conventions by replacing underscores (`_`) with hyphens (`-`).
    - **`catalogImpl`**: The implementation class for the Iceberg catalog (`org.apache.iceberg.aws.glue.GlueCatalog`). This means that AWS Glue will be used for metadata management.
    - **`databaseName`**: The database within the catalog (`airlines`), which will store related tables.
    - **`catalogProperties`**: A map that contains properties required for configuring the catalog.

    #### 2. Catalog Properties
    ```java
    catalogProperties.put("type", "iceberg");
    catalogProperties.put("warehouse", "s3://" + bucketName + "/warehouse");
    catalogProperties.put("catalog-impl", catalogImpl);
    catalogProperties.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
    catalogProperties.put("glue.skip-archive", "true");
    catalogProperties.put("glue.region", awsRegion);
    ```
    - **`type`**: Defines the catalog type as `iceberg`.
    - **`warehouse`**: Specifies the warehouse location in Amazon S3 (`s3://<bucketName>/warehouse`). This is where Iceberg tables' data will be stored.
    - **`catalog-impl`**: Specifies the implementation (`GlueCatalog`) to use for managing metadata.
    - **`io-impl`**: Specifies the I/O implementation (`S3FileIO`) to read from and write to Amazon S3.
    - **`glue.skip-archive`**: By setting `"true"`, Glue can skip archiving old table metadata, making operations faster.
    - **`glue.region`**: Sets the AWS region for AWS Glue.

    #### 3. Creating a CatalogLoader
    ```java
    CatalogLoader catalogLoader = CatalogLoader.custom(catalogName, catalogProperties, new Configuration(false), catalogImpl);
    ```
    - **`CatalogLoader`**: This class is used to load the Iceberg catalog. The custom catalog loader is created using the provided catalog properties.
    - **Parameters**:
      - **`catalogName`**: The name of the catalog.
      - **`catalogProperties`**: Properties that define the configuration (e.g., type, warehouse location, etc.).
      - **`new Configuration(false)`**: Represents the Hadoop configuration (used here with `false` indicating no default configuration is loaded).
      - **`catalogImpl`**: The implementation to use, in this case, Glue.

    #### 4. Registering and Using the Catalog in Flink
    ```java
    CatalogDescriptor catalogDescriptor = CatalogDescriptor.of(catalogName, org.apache.flink.configuration.Configuration.fromMap(catalogProperties));
    tblEnv.createCatalog(catalogName, catalogDescriptor);
    tblEnv.useCatalog(catalogName);
    org.apache.flink.table.catalog.Catalog catalog = tblEnv.getCatalog("apache_kickstarter").orElseThrow(() -> new RuntimeException("Catalog not found"));
    ```
    - **`CatalogDescriptor`**: This class is used to describe and configure the Iceberg catalog for Flink’s Table API.
      - **`of(catalogName, Configuration.fromMap(catalogProperties))`**: Creates a catalog descriptor using the provided name and configuration.
      
    - **Creating and Registering Catalog**:
      - **`tblEnv.createCatalog(catalogName, catalogDescriptor)`**: Registers the catalog with the specified name (`catalogName`) in the `StreamTableEnvironment` (`tblEnv`). This makes the catalog available for use within the Flink environment.
      - **`tblEnv.useCatalog(catalogName)`**: Sets the newly created catalog as the current catalog in use, meaning any subsequent table-related commands will reference this catalog.
      
    - **Retrieving the Catalog**:
      - **`tblEnv.getCatalog("apache_kickstarter")`**: Retrieves the registered catalog from the environment.
      - **`orElseThrow(() -> new RuntimeException("Catalog not found"))`**: Throws an exception if the catalog with the given name cannot be found, providing error handling.

#### 1. Checking if the Database Exists and Creating It if Necessary
```java
try {
    if (!catalog.databaseExists(databaseName)) {
        catalog.createDatabase(databaseName, new CatalogDatabaseImpl(new HashMap<>(), "The Airlines flight data database."), false);
    }
    tblEnv.useDatabase(databaseName);
} catch (Exception e) {
    System.out.println("A critical error occurred during the processing of the database because " + e.getMessage());
    e.printStackTrace();
    System.exit(1);
}
```
- **`catalog.databaseExists(databaseName)`**: Checks if the database (`databaseName`—in this case, `"airlines"`) already exists in the catalog.
- **If the database does not exist**:
  - **`catalog.createDatabase()`**: Creates a new database using the `createDatabase()` method.
    - **Parameters**:
      - **`databaseName`**: The name of the database to be created (`airlines`).
      - **`new CatalogDatabaseImpl(new HashMap<>(), "The Airlines flight data database.")`**: 
        - `CatalogDatabaseImpl` is used to represent the new database.
        - **`new HashMap<>()`**: Provides properties for the database (an empty map here).
        - **`"The Airlines flight data database."`**: Provides a description for the database.
      - **`false`**: Indicates that the method should throw an error if the database already exists (though, in this case, it’s guarded by the `if` statement).
- **`tblEnv.useDatabase(databaseName)`**: Sets the `airlines` database as the current database in the `StreamTableEnvironment` (`tblEnv`).
- **Exception Handling**:
  - If there’s an error during this process, it catches the exception, prints the error message, and calls `System.exit(1)` to terminate the program with an error status.

#### 2. Print the Current Database Name
```java
System.out.println("Current database: " + tblEnv.getCurrentDatabase());
```
- **`tblEnv.getCurrentDatabase()`**: Retrieves the name of the current database that Flink is using.
- This line prints the current database to confirm that the desired database (`airlines`) has been set successfully.

#### 3. Define the RowType for the RowData
```java
RowType rowType = RowType.of(
    new LogicalType[] {
        DataTypes.STRING().getLogicalType(),
        DataTypes.STRING().getLogicalType(),
        DataTypes.STRING().getLogicalType(),
        DataTypes.STRING().getLogicalType(),
        DataTypes.STRING().getLogicalType(),
        DataTypes.BIGINT().getLogicalType(),
        DataTypes.STRING().getLogicalType(),
        DataTypes.STRING().getLogicalType(),
        DataTypes.DECIMAL(10, 2).getLogicalType(),
        DataTypes.STRING().getLogicalType(),
        DataTypes.STRING().getLogicalType()
    },
    new String[] {
        "email_address",
        "departure_time",
        "departure_airport_code",
        "arrival_time",
        "arrival_airport_code",
        "flight_duration",
        "flight_number",
        "confirmation_code",
        "ticket_price",
        "aircraft",
        "booking_agency_email"
    }
);
```
- **`RowType`**: Defines the schema for the rows of data that will be used in the Flink data stream and written to Iceberg tables.
- **`RowType.of(LogicalType[], String[])`**:
  - **`LogicalType[]`**: Defines the data types for each field in the row. Here’s the breakdown:
    - **`DataTypes.STRING().getLogicalType()`**: Represents fields such as `email_address`, `departure_time`, `flight_number`, etc., that are of type `STRING`.
    - **`DataTypes.BIGINT().getLogicalType()`**: Represents the `flight_duration` field, which is of type `BIGINT`.
    - **`DataTypes.DECIMAL(10, 2).getLogicalType()`**: Represents the `ticket_price` field, with a precision of 10 and scale of 2, making it suitable for storing currency values.
  - **`String[]`**: Defines the names of the fields:
    - `"email_address"`, `"departure_time"`, `"departure_airport_code"`, etc.
    - The names correspond to columns that will be defined in the Iceberg table.

6. **SinkToIcebergTable Method**
   - A utility method that takes the input data stream, transforms it to `RowData`, and writes it to the appropriate Iceberg table.
   - If the Iceberg table does not exist, it creates the table and sets properties like `partitioning`, `format-version`, and `target-file-size`.
   - The method uses `FlinkSink.forRowData()` to write the data stream to Iceberg tables in `UPSERT` mode, ensuring that updates are handled properly.

### Key Features and Highlights

1. **Data Streaming to Kafka and Iceberg**
   - **Dual Sink Strategy**: The application sinks data into Kafka (for streaming use cases) and Apache Iceberg (for analytical and historical use cases).
   - **Integration with AWS Glue**: The AWS Glue catalog is used to manage metadata for Iceberg, allowing for tight integration with AWS services.

2. **Scalable Data Generation and Streaming**
   - The `DataGeneratorSource` with rate limiting (`RateLimiterStrategy.perSecond(1)`) is used to generate realistic streaming data.
   - **Fault Tolerance**: Checkpointing, along with Iceberg’s snapshot-based architecture, ensures that the data pipeline can recover and provide consistency in case of a failure.

3. **Efficient Data Management with Iceberg**
   - **Catalog and Metadata**: By utilizing AWS Glue, the application benefits from Iceberg's capabilities for managing schemas, partitioning, and metadata centrally.
   - **Table Partitioning**: Data in Iceberg is partitioned by `arrival_airport_code` to improve query performance, especially for analytics queries on specific routes.
   - **Upserts and Time Travel**: The use of Iceberg's upsert feature (`upsert(true)`) allows seamless updates, and Iceberg’s time-travel functionality is implicitly available, enabling historical queries and analysis.

4. **Seamless Integration of Technologies**
   - The combination of **Apache Flink** (for real-time data processing), **Apache Kafka** (for real-time messaging), and **Apache Iceberg** (for long-term storage and historical analysis) creates a modern **data pipeline** that supports both streaming and batch workloads.
   - **AWS Services Integration**: With Glue and S3, the solution leverages AWS for both metadata management and scalable storage, creating a cost-effective cloud-native architecture.

5. **Code Reusability and Modularity**
   - The `SinkToIcebergTable` method is designed to be generic, allowing different data streams to be easily written to different Iceberg tables.
   - **MapFunction for Data Transformation**: The transformation from `AirlineData` to `RowData` is implemented using a reusable `MapFunction`, which makes the solution extendable to other data structures.

   This function is part of the Apache Flink streaming pipeline, and it converts a data stream of type `AirlineData` into a data stream of type `RowData`. Specifically, it:

    1. **DataStream Mapping**: Uses the `.map()` transformation to convert each element in the `DataStream<AirlineData>` to an element of type `RowData`. This is done by defining an anonymous implementation of the `MapFunction<AirlineData, RowData>` interface.

    2. **Conversion Logic**:
      - The input type is `AirlineData`, which is assumed to be a POJO (Plain Old Java Object) representing a flight, containing fields such as `emailAddress`, `departureTime`, etc.
      - A new `GenericRowData` object (`rowData`) is created to represent the transformed data in a structured, table-like format (`RowData`).
      - **RowKind**: The `GenericRowData` is instantiated with `RowKind.INSERT`, indicating that the operation is an **insertion** (this is relevant when dealing with upserts or changelogs in data streams).
      - The fields of the `rowData` are then populated with values from the `AirlineData` object. Each field is set according to its position in the `GenericRowData`.
        - The fields are set by extracting corresponding values from `AirlineData` and converting them to the appropriate data types (`StringData`, `DecimalData`, etc.).
        - For example, the `emailAddress` field is converted to `StringData` and assigned to the first index (0) of the `GenericRowData`.

    3. **Transformation Details**:
      - **String Fields**: Fields like `emailAddress`, `departureTime`, and `flightNumber` are converted from Java `String` to `StringData` using `StringData.fromString()`. This ensures that Flink handles the data in a consistent format that can be efficiently used by other parts of the Flink system.
      - **Numeric Fields**: The `flightDuration` is set as-is, while the `ticketPrice` is converted to a `DecimalData` type with a precision of `10, 2` to accurately represent currency values.
      - The transformed `RowData` is then returned for each record in the `airlineDataStream`.

    4. **Output**: The result is a `DataStream<RowData>` (`skyOneRowData`), where each record is a `RowData` instance representing a row in a tabular format, containing fields like `email_address`, `departure_time`, and `ticket_price`. This transformed stream (`skyOneRowData`) can then be used by other components in the Flink pipeline, such as a sink to write to Apache Iceberg.

    This code snippet integrates Apache Flink with Apache Iceberg, performing the following operations:
    
    ### 1. Set Up a `TableIdentifier`
    ```java
    TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);
    ```
    - **`TableIdentifier`**: Represents a unique identifier for the Iceberg table, which consists of the `databaseName` and `tableName`.
    - This helps identify the specific Iceberg table that the data will be written to.

    ### 2. Create the Table If It Does Not Exist
    ```java
    if (!catalog.tableExists(ObjectPath.fromString(databaseName + "." + tableName))) {
        tblEnv.executeSql(
                    "CREATE TABLE " + databaseName + "." + tableName + " ("
                        + "email_address STRING, "
                        + "departure_time STRING, "
                        + "departure_airport_code STRING, "
                        + "arrival_time STRING, "
                        + "arrival_airport_code STRING, "
                        + "flight_duration BIGINT,"
                        + "flight_number STRING, "
                        + "confirmation_code STRING, "
                        + "ticket_price DECIMAL(10,2), "
                        + "aircraft STRING, "
                        + "booking_agency_email STRING) "
                        + "WITH ("
                            + "'write.format.default' = 'parquet',"
                            + "'write.target-file-size-bytes' = '134217728',"
                            + "'partitioning' = 'arrival_airport_code',"
                            + "'format-version' = '2');"
                );
    }
    ```
    - **`catalog.tableExists()`**: Checks if the table already exists in the given Iceberg catalog using the `ObjectPath` formed from `databaseName` and `tableName`.
    - If the table **does not exist**:
      - **SQL Execution**: Uses `tblEnv.executeSql()` to run an SQL `CREATE TABLE` statement.
      - **Table Schema**:
        - Defines columns like `email_address`, `departure_time`, `flight_number`, etc.
        - Specifies the data types (e.g., `STRING`, `BIGINT`, `DECIMAL`).
      - **Table Properties**:
        - **`write.format.default`**: Specifies the file format as `parquet` for writing data.
        - **`write.target-file-size-bytes`**: Sets a target file size of 128 MB (`134217728` bytes) to optimize read and write performance.
        - **`partitioning`**: Partitions the table by the column `arrival_airport_code`. Partitioning helps speed up queries by avoiding full table scans.
        - **`format-version`**: Specifies Iceberg table format version (`version 2`), which includes additional features like row-level operations.

    ### 3. Load the Iceberg Table
    ```java
    TableLoader tableLoaderSkyOne = TableLoader.fromCatalog(catalogLoader, tableIdentifier);
    ```
    - **`TableLoader`**: Used to load the specified Iceberg table from the catalog.
      - **`fromCatalog(catalogLoader, tableIdentifier)`**: Loads the table using the previously defined `catalogLoader` and `tableIdentifier`. The `catalogLoader` knows how to connect to the metadata (managed by AWS Glue in this case).

    ### 4. Sink Data to the Iceberg Table
    ```java
    FlinkSink
        .forRowData(skyOneRowData)
        .tableLoader(tableLoaderSkyOne)
        .upsert(true)
        .equalityFieldColumns(Arrays.asList("email_address", "departure_airport_code", "arrival_airport_code"))
        .append();
    ```
    - **`FlinkSink.forRowData()`**: Configures a sink specifically for `RowData` that needs to be written to Iceberg.
      - **`skyOneRowData`**: The input data stream, which is in `RowData` format, is generated from a stream of `AirlineData` using a `MapFunction` (not shown in this snippet, but described earlier).
    - **`tableLoader()`**: Specifies the table that the data will be written to by using the `TableLoader`.
    - **`upsert(true)`**: Enables **upsert** semantics, which means that rows will either be inserted or updated based on the key fields specified:
      - **If the key already exists** in the Iceberg table, the row will be **updated**.
      - **If the key does not exist**, the row will be **inserted**.
    - **`equalityFieldColumns()`**: Specifies the columns used to determine uniqueness when upserting.
      - Here, `email_address`, `departure_airport_code`, and `arrival_airport_code` are used as key fields for checking if a record already exists.
    - **`append()`**: Triggers the data insertion into the table. Once the sink is attached to the `skyOneRowData` stream, it will be executed when the job is run.


### Summary
The `DataGeneratorApp` class is a well-rounded Flink application that demonstrates:
- **Data Stream Generation**: Using `DataGeneratorSource` to create realistic flight data.
- **Integration with Kafka and Iceberg**: Publishing the data to Kafka for real-time analytics and to Iceberg for historical analysis.
- **AWS Glue for Metadata Management**: Integrating AWS Glue with Iceberg to manage metadata in a centralized, consistent manner.
- **Resiliency and Fault Tolerance**: Implementing checkpointing and delivery guarantees to ensure the stability and reliability of the data pipeline.

This setup aligns well with modern data architectures like **data lakehouses**, combining the best features of data lakes and data warehouses. It allows data to be processed in real-time, stored efficiently, and analyzed historically, all while maintaining flexibility, scalability, and cost-effectiveness.

## Resources
Tomer Shiran, Jason Hughes & Alex Merced. [Apache Iceberg -- The Definitive Guide](https://www.dremio.com/wp-content/uploads/2023/02/apache-iceberg-TDG_ER1.pdf).  O'Reilly, 2024.
