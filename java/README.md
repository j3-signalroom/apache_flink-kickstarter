# Writing Flink Apps in Java
Examples of Apache Flink® applications showcasing the [DataStream API](https://nightlies.apache.org/flink/flink-docs-master/docs/learn-flink/datastream_api/) and [Table API](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/overview/) in Java, featuring AWS, GitHub, Terraform, and Apache Iceberg.

## What's the difference between the DataStream API and Table API?
**TL:DR;**  The Apache Flink DataStream API is more flexible and offers low-level control.  It is better suited for complex, low-level stream processing tasks.  In contrast, the Apache Flink Table API (and, by extension, Flink SQL) is more high-level and easier to use, especially for developers familiar with relational data processing. This makes it great for typical structured data processing and analytics.

Apache Flink provides two primary APIs for stream processing: the **DataStream API** and the **Table API**.  Both are powerful but they serve different purposes and target different user groups.  Here’s a detailed comparison between them:

### 1. **Abstraction Level**:
  - **DataStream API**:
    - **Low-level API**:
      - Provides fine-grained control over stream processing. You work directly with streams of data, applying transformations, windowing, and other operations.
      - Suitable for use cases where you need to define custom operations, transformations, and logic.
      - Ideal for developers who are comfortable with writing detailed code and need flexibility and control.

  - **Table API**:
    - **High-level API** (more declarative):
      - Based on relational concepts, it allows users to express their logic using SQL-like operations or in a tabular manner.
      - Best suited for users who prefer working with tables or relational data structures and want to avoid the complexity of the lower-level stream processing logic.
      - Designed for both stream and batch processing with the same semantics, making it easier to switch between the two modes.

### 2. **Programming Style**:
   - **DataStream API**:
     - Functional programming style where you define transformations such as `map`, `flatMap`, `filter`, and `keyBy`.
     - The focus is on how the data should be transformed step by step.

   - **Table API**:
     - Declarative programming style.
     - You define what you want to compute (e.g., filters, aggregations) in terms of SQL or expressions on tables. Flink handles the underlying stream processing automatically.

### 3. **Target Users**:
   - **DataStream API**:
     - More suitable for developers who need full control over the stream processing pipeline.
     - Typically used by experienced developers or those who need to implement complex, custom transformations, or fine-tuned performance optimizations.

   - **Table API**:
     - Targets data engineers, analysts, or users with a background in SQL or relational databases.
     - Suitable for those who want to work with structured data and express computations using familiar SQL-like syntax.

### 4. **Data Model**:
   - **DataStream API**:
     - Operates on unbounded streams of events, represented as arbitrary Java or Scala objects.
     - The user is responsible for defining the schema or structure of the data if needed.

   - **Table API**:
     - Operates on tables, which are logically similar to relational database tables. Tables can represent either static (batch) or dynamic (stream) data.
     - The schema is well-defined, with columns and data types.

### 5. **Operations**:
   - **DataStream API**:
     - Offers a wide range of transformations like `map`, `flatMap`, `filter`, `reduce`, `window`, and `keyBy`.
     - Allows complex event-driven patterns, custom windowing, and stateful operations.

   - **Table API**:
     - Provides a rich set of relational operations like `select`, `filter`, `join`, `groupBy`, and `aggregate`.
     - SQL queries can be executed directly on streams or tables.
     - It is limited to the set of operations that can be expressed in relational terms, making it more restrictive than the DataStream API but much simpler to use for typical analytics use cases.

### 6. **Integration with SQL**:
   - **DataStream API**:
     - It does not natively support SQL queries, although SQL-based operations can be done with additional effort.
     - Requires imperative coding for operations that SQL could otherwise handle declaratively.

   - **Table API**:
     - Integrated with the **Flink SQL API**, allowing you to execute SQL queries on streams or batch data.
     - Perfect for users who want to leverage the power of SQL for data processing.

### 7. **Use Cases**:
   - **DataStream API**:
     - Ideal for complex event processing, real-time analytics, machine learning pipelines, and scenarios where detailed control over the data processing is necessary.
     - Suitable for unstructured or semi-structured data where the user needs to define the transformation logic.

   - **Table API**:
     - Best for typical relational data processing tasks, like filtering, joining, or aggregating structured data in a SQL-like fashion.
     - Well-suited for ETL pipelines, data analytics, or streaming applications where SQL-style queries are sufficient.

### 8. **Performance**:
   - **DataStream API**:
     - Offers fine-tuned performance for custom transformations and windowing logic.
     - More flexible in optimizing specific operations, though it requires manual optimization.

   - **Table API**:
     - Underlying optimizations are managed by Flink's query optimizer.
     - You benefit from automatic optimizations without needing to manage the details yourself, but you might have less control compared to the DataStream API.

## Try out the Flink Apps
Open the repo from the `Java` subfolder.  Then run:

> *This command ensures a pristine build environment.  By removing previous build artifacts, this command guarantees that developers initiate their projects from a clean slate, minimizing inconsistencies and fostering a more reliable build process.*

```
./gradlew app:clean
```

Now build JAR file that contains all the Flink Apps on it, by running:

```
./gradlew app:build
```

Logon to the `apache_flink-kickstarter-jobmanager-1` container's Interative Shell:
> *This allows you to interact with the container as if you were inside its terminal, enabling you to run commands, inspect the file system, or perform other tasks interactively within the container.*
```
docker exec -it -w /opt/flink/apps apache_flink-kickstarter-jobmanager-1 /bin/bash
```

Finally, to run any of the Flink Apps, choose the app and then enter the corresponding CLI command:

App|Commands for CLI
-|-
**`DataGeneratorApp`**|`flink run --class apache_flink.kickstarter.DataGeneratorApp apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`FlightImporterApp`**|`flink run --class apache_flink.kickstarter.FlightImporterApp apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
**`UserStatisticsApp`**|`flink run --class apache_flink.kickstarter.UserStatisticsApp apache_flink-kickstarter-x.xx.xx.xxx.jar --get-from-aws`
