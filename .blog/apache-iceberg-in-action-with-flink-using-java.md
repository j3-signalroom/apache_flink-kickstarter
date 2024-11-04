# Apache Iceberg in Action with Apache Flink using Java
Data engineering transforms raw data into useful, accessible data products in the Data Mesh platform-building era. Like the signalRoom GenAI Data Mesh platform, we package our data products in Apache Iceberg tables. In this article, I'll take you through sinking your Apache Flink data into Apache Iceberg tables using Java. This is a natural follow-up to my previous short piece, [Apache Flink + Apache Iceberg + AWS Glue: Get Your JAR Versions Right!](https://thej3.com/apache-flink-apache-iceberg-aws-glue-get-your-jar-versions-right-805041abef11) where I listed out the right combination of JARs to use.

In this article, I'll walk you through how to seamlessly sink data in your Flink application to Apache Iceberg tables using AWS Glue as your Apache Iceberg catalog, ensuring reliability, performance, and future-proof data storage. We will do this using the [Apache Flink Kickstarter Data Generator Flink app](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/DataGeneratorApp.java). This app generates synthetic flight data for two fictional airlines (`Sunset Air` and `SkyOne`) and streams it into Apache Kafka and Apache Iceberg. The app provides real-time and historical analytics capabilities, demonstrating the power of Apache Iceberg as a table format for large, complex analytic datasets in distributed data lakehouses.  Moreover, it illustrates how AWS Glue is used as the metadata catalog for the Apache Iceberg tables.

![screenshot-datageneratorapp](images/screenshot-datageneratorapp.png)

So, the article is laid out as follows:

![explain-plan](images/explain-plan.gif)

- What is Apache Iceberg, and why is it a gamechanger for data platform architecture?
- How to set up AWS Glue to use it as your Apache Iceberg catalog.
- Step-by-step walkthrough of Data Generator Flink App, that puts it all together.  

## What is Apache Iceberg?
Apache Iceberg was created in 2017 by Netflix's Ryan Blue and Daniel Weeks. It is an open table format designed to resolve the deficiencies of working with data lakes, especially those on distributed storage systems like Amazon S3, Google Cloud Storage, and Azure Blob Storage. A table format is a method of structuring a dataset’s files to present them as a unified “table.” From the user’s perspective, it can be defined as the answer to the question, "What data is in this table?” However, to implement a table format on a distributed storage system, Apache Iceberg needed to overcome several challenges posed by distributed storage systems (e.g., S3, Google Cloud Storage, and Azure Blob Storage):

Problem|Challenge|Impact|Solution
-|-|-|-
**Lack of Consistency and ACID Guarantees**|Distributed storage systems are typically designed for object storage, not traditional database operations. This leads to issues with consistency, especially during concurrent read and write operations.|Without ACID (Atomicity, Consistency, Isolation, Durability) guarantees, operations like updates, deletes, and inserts can become error-prone, leading to inconsistent data views across different sessions or processes.|Apache Iceberg provides ACID compliance, ensuring reliable data consistency on distributed storage systems.
**Bloated Metatdata Files and Slow Query Performance**|As datasets grow in size, so does the metadata (file paths, schema, partitions) associated with them. Efficiently querying large volumes of metadata can become slow and inefficient.|Simple operations like listing files in a directory can become time-consuming, affecting the performance of queries and applications.|Apache Iceberg organizes data into partitions and adds metadata layers, reducing the need to scan the entire dataset and optimizing query performance. This approach allows for filtering data at the metadata level, which avoids loading unnecessary files.
**Lack of Schema Evolution and Data Mutability**|Analytic datasets often require schema changes (e.g., adding or renaming columns) as business requirements evolve. Distributed storage formats typically lack built-in support for handling schema changes efficiently.|Without schema evolution support, datasets require complex data transformations or complete rewrites, which can be slow and resource-intensive.|Apache Iceberg allows schema changes without reprocessing the entire dataset, making it easy to add new fields or alter existing ones over time.
**Inefficient Partitioning and Data Skipping**|Distributed storage systems don't natively support data partitioning, which is crucial for optimizing queries on large datasets.|Lack of partitioning increases query latency because the system has to scan more data than necessary.|Apache Iceberg allows hidden partitioning and metadata-based pruning, ensuring queries only read the required partitions, reducing scan times and improving performance.
**Lack of Data Versioning and Time Travel**|Many analytic workflows need to access previous data versions for tasks like auditing, debugging, or historical analysis. Distributed storage doesn’t offer built-in support for versioning.|Maintaining multiple versions of the same dataset becomes cumbersome, especially without efficient version control, and can lead to excessive storage costs.|Apache Iceberg offer time travel, allowing users to access snapshots of data at different points in time, providing easy access to historical data.
**Unable to do Concurrent Read and Write Operations**|Large analytic workloads often involve multiple processes reading from and writing to the same data simultaneously. Distributed storage systems do not inherently support these concurrent operations smoothly.|Without proper handling, this can lead to data corruption or version conflicts, especially during high-throughput operations.|Apache Iceberg’s transactional model enables concurrent operations safely by managing snapshots and transactions, ensuring data integrity and consistency.
**Too Many Small Files**|Distributed storage systems can accumulate many small files over time due to frequent appends or updates.|Small files lead to inefficient I/O operations and high metadata costs, degrading query performance and increasing storage costs.|Apache Iceberg handles file compaction as part of data maintenance routines, merging small files into larger ones to optimize storage and access efficiency.

By addressing these challenges, the Apache Iceberg table format enables scalable, high-performance, easy-to-use, and lower-cost data lakehouse solutions (the successor to data lakes), which combine the best of data warehouse and data lake design that leverage distributed storage for both analytic and streaming workloads.

With the challenges resolved by Apache Iceberg when working on a distributed storage system, the question arises: how does it manage the metadata? This is where Apache Iceberg utilizes an engine (i.e., catalog), such as **AWS Glue**, **Hive Megastore**, or **Hadoop** filesystem catalog to track a table’s partitioning, sorting, and schema over time, and so much more using a tree of metadata that an engine can use to plan queries in a fraction of the time it would take with legacy data lake patterns.

![apache-iceberg-table-structure](images/apache-iceberg-table-structure.png)

This metadata tree breaks down the metadata of the table into four components:
- **Manifest file**:  A list of datafiles, containing each datafile’s location/path and key metadata about those datafiles, which allows for creating more efficient execution plans.
- **Manifest list**:  Files that define a single snapshot of the table as a list of manifest files along with stats on those manifests that allow for creating more efficient execution plans.
- **Metadata file**:  Files that define a table’s structure, including its schema, partitioning scheme, and a listing of snapshots.
- **Catalog**:  Tracks the table location (similar to the Hive Metastore), but instead of containing a mapping of table name -> set of directories, it contains a mapping of table name -> location of the table’s most recent metadata file. Several tools, including a Hive Metastore, can be used as a catalog.

### Why Apache Iceberg is a Gamechanger?
The true power of Apache Iceberg is that it allows for the separation of storage from compute independent of a data vendor.  What this means is we are **NO LONGER LOCKED IN** to a single data vendor's compute engine (e.g., **Hive**, **Flink**, **Presto**, **Snowflake**, **Spark**, and **Trino**)!  We store the data independently of the compute engine in our distributed storage system (e.g., Amazon S3, Google Cloud Storage, and Azure Blob Storage), and then we connect to the compute engine that best fits our use case for whatever situation we are using our data in!  Moreover, we could have one copy of the data and use different engines on it for different use cases.  Now, let that sit with you!

![office-mind-blown](images/office-mind-blown.gif)

## AWS Glue for your Apache Iceberg catalog
**AWS Glue** is a fully managed extract, transform, and load (ETL) service offered by Amazon Web Services (AWS). It simplifies the process of preparing and loading data for analytics by automating data discovery, schema inference, and job scheduling. In addition, the AWS Glue Data Catalog serves as a centralized metadata repository for Iceberg tables.

The easiest way to set up AWS Glue in your environment (this article assumes AWS is your cloud provider) is to use Terraform. I mainly use Terraform because it is paramount that CI/CD (Continuous Integration/Continuous Development) be used in any environment to make infrastructure deployment scalable, repeatable, and manageable. What follows is the step-by-step Terraform code you would use to set up the necessary infrastructure for integrating AWS Glue, Amazon S3, and Apache Iceberg, specifically to store Iceberg tables in S3, manage metadata through AWS Glue, and ensure that the appropriate IAM roles and policies are in place for permissions:

### Step 1 of 6.  **Create an S3 Bucket for Apache Tables**
```hcl
resource "aws_s3_bucket" "iceberg_bucket" {
  bucket = <YOUR-UNIQUE-BUCKET-NAME>
}
```
Explanation:
- **`aws_s3_bucket "iceberg_bucket"`** resource: Creates an Amazon S3 bucket for storing the Apache Iceberg Tables.
- **`bucket = <BUCKET-NAME>`**: The bucket name should be unique and adhere to the S3 naming conventions. This bucket will act as the **warehouse** for Apache Iceberg, where all the data/delete files (e.g., Parquet files), and metadata are stored.

### Step 2 of 6.  **Create a Folder within the S3 Bucket**
```hcl
resource "aws_s3_object" "warehouse" {
  bucket = aws_s3_bucket.iceberg_bucket.bucket
  key    = "warehouse/"
}
```
Explanation:
- **`aws_s3_object "warehouse"`**: Creates the S3 folder, which is a human-created concept that organizes objects in the S3 bucket. Note, S3 is an object storage system that stores objects in a flat structure, without a physical folder hierarchy. However, technology like Apache Iceberg requires folder concept, so that is why a folder needs to be create and why S3 provides such a solution.
- **`bucket = aws_s3_bucket.iceberg_bucket.bucket`**: Specifies that this folder object belongs to the previously created `iceberg_bucket`.
- **`key = "warehouse/"`**: Sets the key to represent a folder structure for the Apache Iceberg warehouse.

### Step 3 of 6.  **IAM Role for AWS Glue**
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
Explanation:
- **`aws_iam_role "glue_role"`**: Creates an IAM role for AWS Glue to allow it to interact with other AWS services.
- **`assume_role_policy`**:
  - Defines a **trust policy** that allows the Glue service to assume this role.
  - The principal is set to `"glue.amazonaws.com"`, allowing **AWS Glue** to use the role.

### Step 4 of 6.  **IAM Policy for S3 Access**
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
Explanation:
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

### Step 5 of 6.  **Attach IAM Policy to the Glue Role**
```hcl
resource "aws_iam_role_policy_attachment" "glue_policy_attachment" {
  role       = aws_iam_role.glue_role.name
  policy_arn = aws_iam_policy.glue_s3_access_policy.arn
}
```
Explanation:
- **`aws_iam_role_policy_attachment "glue_policy_attachment"`**: Attaches the previously created S3 access policy (`glue_s3_access_policy`) to the AWS Glue role (`glue_role`).
- **`role`**: Specifies the name of the IAM role to which the policy will be attached.
- **`policy_arn`**: Specifies the ARN of the policy being attached to the role.

### Step 6 of 6.  **Glue Catalog Database for Iceberg Metadata**
```hcl
resource "aws_glue_catalog_database" "iceberg_db" {
  name = "iceberg_database"
}
```
Explanation:
- **`aws_glue_catalog_database "iceberg_db"`**: Creates a new **AWS Glue catalog database** named `"iceberg_database"`.
- This database will be used to store the metadata for Iceberg tables, providing schema management, partitioning information, and other table-level metadata.

#### Summary
- **S3 Bucket and Warehouse Directory**:
  - Creates an S3 bucket (`iceberg_bucket`) to act as the data warehouse for Apache Iceberg.
  - Creates a placeholder object (`warehouse`) to represent the warehouse directory in the bucket.
- **AWS Glue Role and Policy**:
  - **`glue_role`**: Creates an IAM role that allows AWS Glue to interact with S3.
  - **`glue_s3_access_policy`**: Defines the permissions needed for Glue to read/write to the S3 bucket (`GetObject`, `PutObject`, `ListBucket`).
  - The role and policy are then attached to ensure Glue has the appropriate permissions to perform ETL jobs that involve reading from and writing to the Iceberg warehouse in S3.
- **AWS Glue Catalog Database**:
  - Creates an AWS Glue database (`iceberg_db`) to manage the metadata of Apache Iceberg tables.
  - This database will be used by Apache Iceberg for managing the table schemas and providing easy integration with other AWS services for querying and managing datasets.

This Terraform code is an integral part of setting up an **AWS S3 Bucket**, **AWS Glue** and **Apache Iceberg** infrastructure that can be used for managing metadata, storing data files in S3, and providing permissions for AWS Glue to manage the lifecycle of Apache Iceberg tables. The setup is ideal for implementing data lakehouse solutions that need efficient metadata handling and seamless integration with AWS services.

## Putting all together in the Data Generator Flink App
This class, `DataGeneratorApp`, is a comprehensive example of a Flink application that generates synthetic flight datafor two fictional airlines (`Sunset Air` and `Sky One`) and stream this data into Kafka topics and Apache Iceberg tables, and provides both real-time and historical analytics capabilities.  Using Apache Flink to build a streaming pipeline with **Flink DataStream API** and **Apache Iceberg** integration using **AWS Glue**.  Below is a code breakdown of the step-by-step, how I built the application:

### Step 1 of 15.  Retrieve Command Line Arguments
```java
String serviceAccountUser = Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER);
String awsRegion = Common.getAppArgumentValue(args, Common.ARG_AWS_REGION);
```
Explanation:
- **Command Line Arguments**: Retrieves the `serviceAccountUser` and `awsRegion` values from the command line arguments.
  - These values are used later for configuration, like accessing AWS services or following S3 naming conventions.

### 2 of 15.  Set Up Flink Execution Environment
```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
env.enableCheckpointing(5000);
env.getCheckpointConfig().setCheckpointTimeout(60000);
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
```
Explanation:
- **Execution Environment**: Creates the Flink `StreamExecutionEnvironment`, which represents the Flink job's DAG (Directed Acyclic Graph).
- **Checkpointing**:
  - **`enableCheckpointing(5000)`**: Enables checkpointing every 5 seconds to ensure fault tolerance.
  - **Checkpoint Timeout**: Sets a timeout of 60 seconds for each checkpoint (`setCheckpointTimeout(60000)`).
  - **Max Concurrent Checkpoints**: Limits concurrent checkpoints to one (`setMaxConcurrentCheckpoints(1)`), ensuring that only one checkpoint is taken at a time.

```java
EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
StreamTableEnvironment tblEnv = StreamTableEnvironment.create(env, settings);
```
Explanation:
- **Table Environment**: Creates a `StreamTableEnvironment` (`tblEnv`) to work with Flink's Table API, which allows for SQL-like operations and integration with other data processing systems.

### Step 3 of 15.  Retrieve Kafka Properties
```java
DataStream<Properties> dataStreamProducerProperties = 
    env.fromData(new Properties())
       .map(new KafkaClientPropertiesLookup(false, serviceAccountUser))
       .name("kafka_producer_properties");
Properties producerProperties = new Properties();
```
Explanation:
- **Kafka Properties Lookup**: Uses `KafkaClientPropertiesLookup` to fetch the Kafka properties (e.g., broker addresses, security settings) from AWS services (like AWS Secrets Manager).  This is a custom source data stream I built,  (I will explain it in more detail in an upcoming article.) 
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
Explanation:
- **Execute and Collect**: Executes the data stream and collects the Kafka properties. This step is required to ensure that the `producerProperties` are available for setting up the Kafka sinks.
- **Error Handling**: If any exception occurs during this process, the application prints the error, logs it, and exits with a non-zero status.

### Step 4 of 15.  Create Data Sources
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
Explanation:
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
Explanation:
- **Data Generator for Sunset Air**: Similarly, creates a data generator for `Sunset Air` airline.
- **Create Data Stream**: Converts the data source into a `DataStream<AirlineData>` named `sunsetStream`.

### Step 5 of 6. Create Kafka Sinks
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
Explanation:
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
Explanation:
- **Serialization Schema**: Similarly, creates a serializer for the `Sunset Air` data to be published to the `airline.sunset` topic.
- **Kafka Sink**: Sets up the Kafka sink for `Sunset Air` with the same configurations as `Sky One`.

### Step 6 of 15.  Attach Sources and Sinks to Flink's DAG
```java
skyOneStream.sinkTo(skyOneSink).name("skyone_sink");
sunsetStream.sinkTo(sunsetSink).name("sunset_sink");
```
Explanation:
- **Attach Sinks**: Adds the `KafkaSink` for both `Sky One` and `Sunset Air` to the Flink data streams (`skyOneStream` and `sunsetStream`).
  - Only streams with sinks attached will be executed when the `StreamExecutionEnvironment.execute()` method is called.

### Step 7 of 15.  Setting Up Iceberg Catalog Configuration
```java
String catalogName = "apache_kickstarter";
String bucketName = serviceAccountUser.replace("_", "-");  // --- To follow S3 bucket naming convention, replace underscores with hyphens if exist in string.
String catalogImpl = "org.apache.iceberg.aws.glue.GlueCatalog";
String databaseName = "airlines";
Map<String, String> catalogProperties = new HashMap<>();
```
Explanation:
- **`catalogName`**: The name of the Iceberg catalog (`apache_kickstarter`), which will be used to reference this catalog in the Flink environment.
- **`bucketName`**: The S3 bucket where the data will be stored. The code ensures the bucket name follows S3 naming conventions by replacing underscores (`_`) with hyphens (`-`).
- **`catalogImpl`**: The implementation class for the Iceberg catalog (`org.apache.iceberg.aws.glue.GlueCatalog`). This means that AWS Glue will be used for metadata management.
- **`databaseName`**: The database within the catalog (`airlines`), which will store related tables.
- **`catalogProperties`**: A map that contains properties required for configuring the catalog.

### Step 8 of 15.  Catalog Properties
```java
catalogProperties.put("type", "iceberg");
catalogProperties.put("warehouse", "s3://" + bucketName + "/warehouse");
catalogProperties.put("catalog-impl", catalogImpl);
catalogProperties.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
catalogProperties.put("glue.skip-archive", "true");
catalogProperties.put("glue.region", awsRegion);
```
Explanation:
- **`type`**: Defines the catalog type as `iceberg`.
- **`warehouse`**: Specifies the warehouse location in Amazon S3 (`s3://<bucketName>/warehouse`). This is where Iceberg tables' data will be stored.
- **`catalog-impl`**: Specifies the implementation (`GlueCatalog`) to use for managing metadata.
- **`io-impl`**: Specifies the I/O implementation (`S3FileIO`) to read from and write to Amazon S3.
- **`glue.skip-archive`**: By setting `"true"`, Glue can skip archiving old table metadata, making operations faster.
- **`glue.region`**: Sets the AWS region for AWS Glue.

### 9 of 15.  Creating a CatalogLoader
```java
CatalogLoader catalogLoader = CatalogLoader.custom(catalogName, catalogProperties, new Configuration(false), catalogImpl);
```
Explanation:
- **`CatalogLoader`**: This class is used to load the Iceberg catalog. The custom catalog loader is created using the provided catalog properties.
- **Parameters**:
  - **`catalogName`**: The name of the catalog.
  - **`catalogProperties`**: Properties that define the configuration (e.g., type, warehouse location, etc.).
  - **`new Configuration(false)`**: Represents the Hadoop configuration (used here with `false` indicating no default configuration is loaded).
  - **`catalogImpl`**: The implementation to use, in this case, Glue.

### 10 of 15.  Registering and Using the Catalog in Flink
```java
CatalogDescriptor catalogDescriptor = CatalogDescriptor.of(catalogName, org.apache.flink.configuration.Configuration.fromMap(catalogProperties));
tblEnv.createCatalog(catalogName, catalogDescriptor);
tblEnv.useCatalog(catalogName);
org.apache.flink.table.catalog.Catalog catalog = tblEnv.getCatalog("apache_kickstarter").orElseThrow(() -> new RuntimeException("Catalog not found"));
```
Explanation:
- **`CatalogDescriptor`**: This class is used to describe and configure the Iceberg catalog for Flink’s Table API.
  - **`of(catalogName, Configuration.fromMap(catalogProperties))`**: Creates a catalog descriptor using the provided name and configuration.
  
- **Creating and Registering Catalog**:
  - **`tblEnv.createCatalog(catalogName, catalogDescriptor)`**: Registers the catalog with the specified name (`catalogName`) in the `StreamTableEnvironment` (`tblEnv`). This makes the catalog available for use within the Flink environment.
  - **`tblEnv.useCatalog(catalogName)`**: Sets the newly created catalog as the current catalog in use, meaning any subsequent table-related commands will reference this catalog.
  
- **Retrieving the Catalog**:
  - **`tblEnv.getCatalog("apache_kickstarter")`**: Retrieves the registered catalog from the environment.
  - **`orElseThrow(() -> new RuntimeException("Catalog not found"))`**: Throws an exception if the catalog with the given name cannot be found, providing error handling.

### Step 11 of 15.  Checking if the Database Exists and Creating It if Necessary
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
Explanation:
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

### Step 12 of 15.  Print the Current Database Name
```java
System.out.println("Current database: " + tblEnv.getCurrentDatabase());
```
Explanation:
- **`tblEnv.getCurrentDatabase()`**: Retrieves the name of the current database that Flink is using.
- This line prints the current database to confirm that the desired database (`airlines`) has been set successfully.

### Step 13 of 15.  Define the RowType for the RowData
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
Explanation:
- **`RowType`**: Defines the schema for the rows of data that will be used in the Flink data stream and written to Iceberg tables.
- **`RowType.of(LogicalType[], String[])`**:
  - **`LogicalType[]`**: Defines the data types for each field in the row. Here’s the breakdown:
    - **`DataTypes.STRING().getLogicalType()`**: Represents fields such as `email_address`, `departure_time`, `flight_number`, etc., that are of type `STRING`.
    - **`DataTypes.BIGINT().getLogicalType()`**: Represents the `flight_duration` field, which is of type `BIGINT`.
    - **`DataTypes.DECIMAL(10, 2).getLogicalType()`**: Represents the `ticket_price` field, with a precision of 10 and scale of 2, making it suitable for storing currency values.
  - **`String[]`**: Defines the names of the fields:
    - `"email_address"`, `"departure_time"`, `"departure_airport_code"`, etc.
    - The names correspond to columns that will be defined in the Iceberg table.

### Step 14 of 15. **SinkToIcebergTable Method**
   - A utility method that takes the input data stream, transforms it to `RowData`, and writes it to the appropriate Iceberg table.
   - If the Iceberg table does not exist, it creates the table and sets properties like `partitioning`, `format-version`, and `target-file-size`.
   - The method uses `FlinkSink.forRowData()` to write the data stream to Iceberg tables in `UPSERT` mode, ensuring that updates are handled properly.

#### Step 14a of 15.
```java
// --- Convert DataStream<AirlineData> to DataStream<RowData>
DataStream<RowData> skyOneRowData = airlineDataStream.map(new MapFunction<AirlineData, RowData>() {
    @Override
    public RowData map(AirlineData airlineData) throws Exception {
        GenericRowData rowData = new GenericRowData(RowKind.INSERT, fieldCount);
        rowData.setField(0, StringData.fromString(airlineData.getEmailAddress()));
        rowData.setField(1, StringData.fromString(airlineData.getDepartureTime()));
        rowData.setField(2, StringData.fromString(airlineData.getDepartureAirportCode()));
        rowData.setField(3, StringData.fromString(airlineData.getArrivalTime()));
        rowData.setField(4, StringData.fromString(airlineData.getArrivalAirportCode()));
        rowData.setField(5, airlineData.getFlightDuration());
        rowData.setField(6, StringData.fromString(airlineData.getFlightNumber()));
        rowData.setField(7, StringData.fromString(airlineData.getConfirmationCode()));
        rowData.setField(8, DecimalData.fromBigDecimal(airlineData.getTicketPrice(), 10, 2));
        rowData.setField(9, StringData.fromString(airlineData.getAircraft()));
        rowData.setField(10, StringData.fromString(airlineData.getBookingAgencyEmail()));
        return rowData;
    }
});
```
Explanation:
-  Transforms each `AirlineData` object in the input data stream into a `GenericRowData` (`RowData` type).  The `RowData` is used downstream in Apache Flink for writing to sinks.  Essentially, it allows converting structured Java objects into a form that can be integrated with Flink's table APIs, which are better suited for processing and querying structured data.

#### Step 14b of 15. Set Up a `TableIdentifier`
```java
TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);
```
Explanation:
- **`TableIdentifier`**: Represents a unique identifier for the Iceberg table, which consists of the `databaseName` and `tableName`.
- This helps identify the specific Iceberg table that the data will be written to.

#### Step 14c of 15:  Create the Table If It Does Not Exist
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
Explanation:
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

#### Step 14d of 15.  Load the Iceberg Table
```java
TableLoader tableLoaderSkyOne = TableLoader.fromCatalog(catalogLoader, tableIdentifier);
```
Explanation:
- **`TableLoader`**: Used to load the specified Iceberg table from the catalog.
  - **`fromCatalog(catalogLoader, tableIdentifier)`**: Loads the table using the previously defined `catalogLoader` and `tableIdentifier`. The `catalogLoader` knows how to connect to the metadata (managed by AWS Glue in this case).

#### Step 14e of 15.  Sink Data to the Iceberg Table
```java
FlinkSink
    .forRowData(skyOneRowData)
    .tableLoader(tableLoaderSkyOne)
    .upsert(true)
    .equalityFieldColumns(Arrays.asList("email_address", "departure_airport_code", "arrival_airport_code"))
    .append();
```
Explanation:
- **`FlinkSink.forRowData()`**: Configures a sink specifically for `RowData` that needs to be written to Iceberg.
  - **`skyOneRowData`**: The input data stream, which is in `RowData` format, is generated from a stream of `AirlineData` using a `MapFunction` (not shown in this snippet, but described earlier).
- **`tableLoader()`**: Specifies the table that the data will be written to by using the `TableLoader`.
- **`upsert(true)`**: Enables **upsert** semantics, which means that rows will either be inserted or updated based on the key fields specified:
  - **If the key already exists** in the Iceberg table, the row will be **updated**.
  - **If the key does not exist**, the row will be **inserted**.
- **`equalityFieldColumns()`**: Specifies the columns used to determine uniqueness when upserting.
  - Here, `email_address`, `departure_airport_code`, and `arrival_airport_code` are used as key fields for checking if a record already exists.
- **`append()`**: Triggers the data insertion into the table. Once the sink is attached to the `skyOneRowData` stream, it will be executed when the job is run.

### Step 15 of 15.  Execute the DAG



### Summary
1. **Main Method - Entry Point**
   - The `main()` method sets up the Flink environment, creates data generators, defines data sinks, and eventually executes the streaming job.
   - The method outlines the following steps:
     1. **Setup Execution Environment**: Configure Flink for streaming jobs, including checkpointing to ensure fault tolerance.
     2. **Configure Kafka Producer**: Retrieve Kafka producer properties from AWS Secrets Manager and set up Kafka sinks for data streams.
     3. **Generate and Stream Flight Data**: Use a `DataGeneratorSource` to generate flight data for `Sky One` and `Sunset Air`.
     4. **Sink Data to Kafka**: Serialize the data in JSON format and publish it to Kafka topics.
     5. **Apache Iceberg Integration**
   - **Catalog Setup**:
     - Iceberg is configured to use `AWS Glue` as the catalog and `Amazon S3` as the data warehouse location.
     - Properties like `catalog-impl` and `io-impl` are set to ensure that Glue is used for metadata and S3 is used for I/O.
   - **Database Creation**: The `airlines` database is created if it doesn’t exist.
   - **Table Creation and Data Sink**:
     - Defines the table schema (`RowType`) that includes fields like `email_address`, `departure_time`, `ticket_price`, etc.
     - Converts `DataStream<AirlineData>` to `DataStream<RowData>`, and writes the resulting stream to Iceberg tables (`skyone_airline` and `sunset_airline`) using the `FlinkSink`.
     6. **Sink Data to Iceberg Table**: Convert the data stream to `RowData` format and write it to Iceberg tables.


The [`DataGeneratorApp`](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/DataGeneratorApp.java) class is a well-rounded Flink application that demonstrates:
- **Data Stream Generation**: Using [`DataGenerator`](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/github_issue-417/java/app/src/main/java/kickstarter/DataGenerator.java) class object to create realistic flight data.
- **Integration with Kafka and Iceberg**: Publishing the data to Kafka for real-time analytics and to Iceberg for historical analysis.
- **AWS Glue for Metadata Management**: Integrating AWS Glue with Iceberg to manage metadata in a centralized, consistent manner.
- **Resiliency and Fault Tolerance**: Implementing checkpointing and delivery guarantees to ensure the stability and reliability of the data pipeline.

This code example embodies the principles of modern data architectures, such as data lakehouses, by seamlessly integrating the strengths of data lakes and data warehouses. It empowers real-time data processing, efficient storage, and in-depth historical analysis—all while offering unmatched flexibility, scalability, and cost-efficiency.

## Resources
Jeffrey Jonathan Jennings.  [Apache Flink + Apache Iceberg + AWS Glue: Get Your JAR Versions Right!](https://thej3.com/apache-flink-apache-iceberg-aws-glue-get-your-jar-versions-right-805041abef11).  Medium, 2024.

Tomer Shiran, Jason Hughes & Alex Merced.  [Apache Iceberg -- The Definitive Guide](https://www.dremio.com/wp-content/uploads/2023/02/apache-iceberg-TDG_ER1.pdf).  O'Reilly, 2024.
