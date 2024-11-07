# Apache Iceberg in Action with Apache Flink using Java
Data engineering transforms raw data into useful, accessible data products in the Data Mesh platform-building era. Like the signalRoom GenAI Data Mesh platform, we package our data products in Apache Iceberg tables. In this article, I’ll take you through sinking your Apache Flink data into Apache Iceberg tables using Java. This is a natural follow-up to my previous short piece, [Apache Flink + Apache Iceberg + AWS Glue: Get Your JAR Versions Right!](get-your-jar-versions-right.md) where I listed out the right combination of JARs to use.

In this article, I’ll walk you through how to seamlessly sink data in your Flink application to Apache Iceberg tables using AWS Glue as your Apache Iceberg catalog, ensuring reliability, performance, and future-proof data storage. We will do this using the [Apache Flink Kickstarter Data Generator Flink app](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/DataGeneratorApp.java). This app generates synthetic flight data for two fictional airlines (`Sunset Air` and `SkyOne`) and streams it into Apache Kafka and Apache Iceberg. The app provides real-time and historical analytics capabilities, demonstrating the power of Apache Iceberg as a table format for large, complex analytic datasets in distributed data lakehouses. Moreover, it illustrates how AWS Glue is used as the metadata catalog for the Apache Iceberg tables.

![screenshot-datageneratorapp](images/screenshot-datageneratorapp.png)

The plan for the remainder of the article is as follows:

![explain-plan](images/explain-plan.gif)

- What is Apache Iceberg, and why is it a game-changer for data platform architecture?
- How to set up AWS Glue to use it as your Apache Iceberg catalog.
- Step-by-step walkthrough of Data Generator Flink App that puts it all together.

## What is Apache Iceberg?
Apache Iceberg was created in 2017 by Netflix’s Ryan Blue and Daniel Weeks. It is an open table format designed to resolve the deficiencies of working with data lakes, especially those on distributed storage systems like Amazon S3, Google Cloud Storage, and Azure Blob Storage. A table format is a method of structuring a dataset’s files to present them as a unified “table.” From the user’s perspective, it can be defined as the answer to the question, “What data is in this table?” However, to implement a table format on a distributed storage system, Apache Iceberg needed to overcome several challenges posed by distributed storage systems (e.g., S3, Google Cloud Storage, and Azure Blob Storage):

Problem|Challenge|Impact|Solution
-|-|-|-
**Lack of Consistency and ACID Guarantees**|Distributed storage systems are typically designed for object storage, not traditional database operations. This leads to issues with consistency, especially during concurrent read and write operations.|Without ACID (Atomicity, Consistency, Isolation, Durability) guarantees, operations like updates, deletes, and inserts can become error-prone, leading to inconsistent data views across different sessions or processes.|Apache Iceberg provides ACID compliance, ensuring reliable data consistency on distributed storage systems.
**Bloated Metatdata Files and Slow Query Performance**|As datasets grow in size, so does the metadata (file paths, schema, partitions) associated with them. Efficiently querying large volumes of metadata can become slow and inefficient.|Simple operations like listing files in a directory can become time-consuming, affecting the performance of queries and applications.|Apache Iceberg organizes data into partitions and adds metadata layers, reducing the need to scan the entire dataset and optimizing query performance. This approach allows for filtering data at the metadata level, which avoids loading unnecessary files.
**Lack of Schema Evolution and Data Mutability**|Analytic datasets often require schema changes (e.g., adding or renaming columns) as business requirements evolve. Distributed storage formats typically lack built-in support for handling schema changes efficiently.|Without schema evolution support, datasets require complex data transformations or complete rewrites, which can be slow and resource-intensive.|Apache Iceberg allows schema changes without reprocessing the entire dataset, making it easy to add new fields or alter existing ones over time.
**Inefficient Partitioning and Data Skipping**|Distributed storage systems don't natively support data partitioning, which is crucial for optimizing queries on large datasets.|Lack of partitioning increases query latency because the system has to scan more data than necessary.|Apache Iceberg allows hidden partitioning and metadata-based pruning, ensuring queries only read the required partitions, reducing scan times and improving performance.
**Lack of Data Versioning and Time Travel**|Many analytic workflows need to access previous data versions for tasks like auditing, debugging, or historical analysis. Distributed storage doesn’t offer built-in support for versioning.|Maintaining multiple versions of the same dataset becomes cumbersome, especially without efficient version control, and can lead to excessive storage costs.|Apache Iceberg offer time travel, allowing users to access snapshots of data at different points in time, providing easy access to historical data.
**Unable to do Concurrent Read and Write Operations**|Large analytic workloads often involve multiple processes reading from and writing to the same data simultaneously. Distributed storage systems do not inherently support these concurrent operations smoothly.|Without proper handling, this can lead to data corruption or version conflicts, especially during high-throughput operations.|Apache Iceberg’s transactional model enables concurrent operations safely by managing snapshots and transactions, ensuring data integrity and consistency.
**Too Many Small Files**|Distributed storage systems can accumulate many small files over time due to frequent appends or updates.|Small files lead to inefficient I/O operations and high metadata costs, degrading query performance and increasing storage costs.|Apache Iceberg handles file compaction as part of data maintenance routines, merging small files into larger ones to optimize storage and access efficiency.

By addressing these challenges, the Apache Iceberg table format enables scalable, high-performance, easy-to-use, and lower-cost data lakehouse solutions (the successor to data lakes). These solutions combine the best of data warehouse and data lake design and leverage distributed storage for both analytic and streaming workloads.

With the challenges resolved by Apache Iceberg when working on a distributed storage system, the question arises: how does it manage the metadata? This is where Apache Iceberg utilizes an engine (i.e., catalog), such as **AWS Glue**, **Hive Megastore**, or **Hadoop** filesystem catalog, to track a table’s partitioning, sorting, and schema over time, and so much more using a tree of metadata that an engine can use to plan queries in a fraction of the time it would take with legacy data lake patterns.

![apache-iceberg-table-structure](images/apache-iceberg-table-structure.png)

This metadata tree breaks down the metadata of the table into four components:

- **Manifest file:** A list of data files containing each data file’s location/path and key metadata about those data files, which allows for creating more efficient execution plans.
- **Manifest list:** Files that define a single snapshot of the table as a list of manifest files and stats on those manifests that allow for creating more efficient execution plans.
- **Metadata file:** Files that define a table’s structure, including its schema, partitioning scheme, and a listing of snapshots.
- **Catalog:** This tool tracks the table location (similar to the Hive Metastore), but instead of containing a mapping of table name -> set of directories, it includes a mapping of table name -> location of the table’s most recent metadata file. Several tools, including a Hive Metastore, can be used as a catalog.

### Why Apache Iceberg is a Game-changer?
The true power of Apache Iceberg is that it allows for the separation of storage from compute. This means we are **NO LONGER LOCKED INTO** a single data vendor’s compute engine (e.g., Hive, **Flink**, Presto, **Snowflake**, **Spark**, and Trino)! We store the data independently of the compute engine in our distributed storage system (e.g., Amazon S3, Google Cloud Storage, and Azure Blob Storage). Then, we connect to the compute engine that best fits our use case for whatever situation we use our data in! Moreover, we could have one copy of the data and use different engines for different use cases. Now, let that sit with you!

![office-mind-blown](images/office-mind-blown.gif)

> Imagine the freedom to choose the most cost-effective solution every time you process your data. Whether Apache Flink is more budget-friendly than Snowflake or vice versa, you have the power to decide! Your data isn’t locked into any specific compute engine, giving you ultimate flexibility to optimize for both performance and cost.

![patrick-mind-blown](images/patrick-mind-blown.gif)

## AWS Glue for your Apache Iceberg Catalog
**AWS Glue** is a fully managed extract, transform, and load (ETL) service offered by Amazon Web Services (AWS). It simplifies preparing and loading data for analytics by automating data discovery, schema inference, and job scheduling. In addition, the AWS Glue Data Catalog serves as a centralized metadata repository for Apache Iceberg Tables.

The easiest way to set up AWS Glue in your environment — assuming AWS is your cloud provider — is to use Terraform. I primarily choose Terraform because it is essential for implementing CI/CD (Continuous Integration/Continuous Development) in any environment. This approach ensures that infrastructure deployment is scalable, repeatable, and manageable.

Below is a step-by-step guide with Terraform code to establish the necessary infrastructure for integrating AWS Glue and Amazon S3 with Apache Iceberg. This setup is designed to store Apache Iceberg tables in S3, manage metadata through AWS Glue, and ensure that the appropriate IAM roles and policies are in place for permissions:

### Step 1 of 5. Create the `<YOUR-UNIQUE-BUCKET-NAME>` S3 Bucket for Apache Iceberg Tables
```hcl
resource "aws_s3_bucket" "iceberg_bucket" {
  bucket = <YOUR-UNIQUE-BUCKET-NAME>
}
```

### Step 2 of 5. Create the `warehouse/` Folder Object within the S3 Bucket
```hcl
resource "aws_s3_object" "warehouse" {
  bucket = aws_s3_bucket.iceberg_bucket.bucket
  key    = "warehouse/"
}
```

### Step 3 of 5. IAM Role for AWS Glue
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

### Step 4 of 5. IAM Policy for S3 Access
```hcl
resource "aws_iam_policy" "glue_s3_access_policy" {
  name = "GlueS3AccessPolicy"  policy = jsonencode({
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

### Step 5 of 5. Attach IAM Policy to the AWS Glue Role
```hcl
resource "aws_iam_role_policy_attachment" "glue_policy_attachment" {
  role       = aws_iam_role.glue_role.name
  policy_arn = aws_iam_policy.glue_s3_access_policy.arn
}
```

This Terraform code is essential for setting up an **AWS S3 Bucket**, **AWS Glue**, and **Apache Iceberg** infrastructure. It is designed for managing metadata, storing data files in S3, and granting permissions for AWS Glue to handle the lifecycle of Apache Iceberg Tables. This setup is ideal for implementing data lakehouse solutions that require efficient metadata management and seamless integration with AWS services.

## Putting it all together with the Data Generator Flink App
This app, [DataGeneratorApp](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/DataGeneratorApp.java), is a comprehensive example of a Flink application that generates synthetic flight data for two fictional airlines (`Sunset Air` and `SkyOne`), and sinks the data to Kafka topics and Apache Iceberg Tables, providing real-time and historical analytics capabilities. It uses Apache Flink to build a streaming pipeline with the **Flink DataStream API** and **Apache Iceberg** integration using **AWS Glue**. Below is the step-by-step breakdown of the code:

### Step 1 of 12. Import classes.
```java
package kickstarter;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.kafka.sink.*;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.*;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.data.*;
import org.apache.flink.table.types.logical.*;
import org.apache.flink.types.*;
import org.apache.flink.table.catalog.*;
import org.apache.iceberg.catalog.*;
import org.apache.iceberg.flink.*;
import org.apache.iceberg.flink.sink.FlinkSink;
import org.apache.hadoop.conf.Configuration;
import java.util.*;
import org.slf4j.*;

import kickstarter.model.*;
```

- Since [`AirlineData`](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/model/AirlineData.java) Java POJO will be used in the code later, we **`import kickstarter.model.*`**

```java
public class DataGeneratorApp {
    private static final Logger logger = LoggerFactory.getLogger(DataGeneratorApp.class);


 /**
  * The main method in a Flink application serves as the entry point of the program, where
  * the Flink DAG is defined.  That is, the execution environment, the creation of the data
  * streams or datasets, apply transformations, and trigger the execution of the application (by
  * sending it to the Flink JobManager).
  * 
  * @param args list of strings passed to the main method from the command line.
  * @throws Exception - The exceptions are forwarded, and are caught by the runtime.  
  * When the runtime catches an exception, it aborts the task and lets the fail-over logic
  * decide whether to retry the task execution.
  */
  public static void main(String[] args) throws Exception {
    ...
  }
}
```
- Place the code in steps 2 through 11 in the **`main()`** method, except for the **`SinkToIcebergTable()`** which goes out of the main() method but stays with the **`DataGeneratorApp`** class.

### Step 2 of 11. Retrieve Command-Line Arguments for the Application
```java
String serviceAccountUser = Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER);
String awsRegion = Common.getAppArgumentValue(args, Common.ARG_AWS_REGION);
```

- Use [`Common.getAppArgumentValue()`](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/Common.java) helper methods to retrieve the `serviceAccountUserwhich` contains the S3 bucket name and `awsRegion` which contains the AWS region name from the command-line of the app, respectively.

### Step 3 of 11. Set Up Flink Execution Environment
```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
env.enableCheckpointing(5000);  // Enables checkpointing every 5 seconds to ensure fault tolerance
env.getCheckpointConfig().setCheckpointTimeout(60000);  // Sets a timeout of 60 seconds for each checkpoint
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);  // Ensuring that only one checkpoint is taken at a time
EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
StreamTableEnvironment tblEnv = StreamTableEnvironment.create(env, settings);
```

- **Stream Execution Environment:** Creates the Flink `StreamExecutionEnvironment`, which represents the Flink job's DAG (Directed Acyclic Graph) and enables typical Apache Flink checkpointing settings.

> _[Apache Flink Checkpointing](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/fault-tolerance/checkpointing/#:~:text=Checkpoints%20allow%20Flink%20to%20recover,Flink's%20streaming%20fault%20tolerance%20mechanism.) is a fault-tolerance mechanism that enables stateful stream processing applications to recover from failures while maintaining exactly-once processing semantics._

- **Table Environment:** Creates a `StreamTableEnvironment (tblEnv)` to work with Flink's Table API, which allows for SQL-like operations and integration with other data processing systems.

### Step 4 of 11. Retrieve Producer Kafka Client Properties
> _This article focuses on Apache Iceberg with AWS Glue Data Catalog and Apache Flink, so I did not cover Confluent Cloud, AWS Secrets, or AWS Systems Manager Parameter Store. For related code, see [`aws-resources.tf`](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/aws-resources.tf)._

```java
DataStream<Properties> dataStreamProducerProperties = 
    env.fromData(new Properties())
       .map(new KafkaClientPropertiesLookup(false, serviceAccountUser))
       .name("kafka_producer_properties");
Properties producerProperties = new Properties();

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

- **Kafka Properties Lookup:** Uses [`KafkaClientPropertiesLookup`](https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/java/app/src/main/java/kickstarter/KafkaClientPropertiesLookup.java) to fetch the Producer Kafka Client properties (e.g., broker addresses, security settings) from AWS services (like AWS Secrets Manager). This is a custom source data stream I built, which I will explain in a later post!

- **Create DataStream:** Creates a `DataStream<Properties>` that contains the Kafka producer properties.

> _What is a [DataStream](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/datastream/overview/)?_
> 
> _The `DataStream API` gets its name from the special `DataStream` class that is used to represent a collection of data in a Flink program. You can think of them as immutable collections of data that can contain duplicates. This data can be finite or unbounded; the API you use to work on them is the same._
>
> _A `DataStream` is similar to a regular Java Collection in terms of usage but is quite different in some key ways. They are immutable, meaning that once they are created, you cannot add or remove elements. You can also not simply inspect the elements inside but only work on them using the `DataStream API` operations, which are also called transformations._

- **Execute and Collect:** Adds newly created Producer Kafka Client Properties (`producerProperties`) to the DAG to ensure they can set up the Kafka sinks.
- **Error Handling:** If any exception occurs during this process, the application prints the error, logs it, and exits a non-zero status.

