# Using Non-Managed Apache Iceberg Tables in Snowflake
When working with Apache Iceberg tables in Snowflake, you generally have two key options for how these tables are administered: **managed** and **non-managed**. Each approach offers distinct trade-offs, operational characteristics, and integration patterns.

### Managed Iceberg Tables
**Overview:**  
Managed Iceberg tables are those whose metadata and file operations are fully orchestrated and maintained by Snowflake. In other words, Snowflake takes on the role of the table’s “catalog,” ensuring that all Iceberg-related components—metadata files, data files, snapshots, and manifests—are organized and optimized by Snowflake’s internal engine.

**Characteristics and Benefits:**

1. **Full Metadata Control by Snowflake:**  
   Snowflake stores and manages all Iceberg metadata files. This centralizes the location and governance of table structure, schema evolution, snapshots, and transaction history. As a result, you don’t have to worry about manually maintaining metadata files, directories, or catalogs externally.

2. **Automated Table Maintenance:**  
   Because Snowflake integrates directly with the Iceberg table structures, tasks such as compaction, snapshot retention, and cleanup are handled internally. Snowflake can transparently optimize data layout and purge expired snapshots, reducing administrative overhead.

3. **High Performance and Seamless Concurrency:**  
   Snowflake’s concurrency, scaling, and optimization capabilities extend to managed Iceberg tables. Query performance often benefits from Snowflake’s built-in optimizations and its transactional guarantees. Multiple users can concurrently read and write without worrying about corrupting metadata or dealing with complex lock mechanisms outside Snowflake.

4. **Single Source of Truth:**  
   Since Snowflake is the system of record for both the data and its metadata, it offers a unified interface. You don’t need external systems (like a Hive Metastore, AWS Glue Catalog, or a separate Iceberg catalog) for maintaining the data. Everything resides under Snowflake’s governance and security model.

**Considerations:**
- Managed tables may come with a higher degree of vendor lock-in since Snowflake is the authoritative source for the metadata.
- You rely on Snowflake’s roadmap for Iceberg-related enhancements and features.


### Non-Managed Iceberg Tables
**Overview:**  
Non-managed Iceberg tables, sometimes known as “external Iceberg tables,” rely on an external metadata store and a file system that you control. Snowflake acts as a query engine over data defined and managed outside of its domain. In essence, the Iceberg table’s “catalog” and metadata are stored elsewhere (e.g., in an external metastore, cloud storage bucket with Iceberg metadata files, or a third-party cataloging system), and Snowflake simply queries the data.

**Characteristics and Benefits:**

1. **External Metadata and Catalog:**  
   You already have a catalog, perhaps a Hive Metastore, AWS Glue Data Catalog, or another standalone Iceberg catalog. Snowflake connects to this external metadata source to understand table schemas, snapshots, and file locations. You can maintain Iceberg metadata where you prefer, possibly alongside other engines and tools.

2. **Greater Flexibility and Interoperability:**  
   Since non-managed tables aren’t tied exclusively to Snowflake’s metadata management, it’s easier to integrate your data lake ecosystem. You can run Spark, Presto, Trino, or other engines on the same underlying Iceberg data, using the same metadata store, giving you a multi-engine, multi-tool environment.

3. **Reduced Vendor Lock-In:**  
   Your data and its metadata are not solely under Snowflake’s control, making it simpler to migrate or share data among different analytics platforms. If you prefer a “bring your own catalog” approach, this can be ideal.

**Considerations:**
- You must handle many operational aspects yourself, such as snapshot cleanup, schema evolution policies, and performance tuning related to Iceberg metadata.  
- Achieving optimal concurrency control or ensuring consistent table states might be more complex, as Snowflake does not automatically handle metadata concurrency or optimization tasks.  
- Query performance may be somewhat dependent on how efficiently Snowflake can access external metadata and how well-maintained your external catalog is.

---

### Choosing Between Managed and Non-Managed
**Managed Tables:**  
- Ideal if you want a “hands-off” approach with simpler maintenance, automatic optimizations, and integration with Snowflake’s existing transactional and governance features.  
- Best for organizations that want to consolidate their data management layer within Snowflake.

**Non-Managed Tables:**  
- Ideal for environments with existing data lake ecosystems and catalogs, where interoperability and flexibility are top priorities.  
- Best for teams comfortable with maintaining Iceberg metadata outside Snowflake and who value the ability to use multiple engines against the same data.

By weighing these considerations, you can determine the right Iceberg table management approach—fully managed by Snowflake for simplicity and performance, or non-managed for ultimate flexibility and ecosystem interoperability.

---

In this project, we use non-managed Apache Iceberg tables in Snowflake. The Apache Iceberg tables are created and managed in Apache Flink, and Snowflake queries the data from these tables. This approach allows us to leverage the best of both worlds: Apache Iceberg for data processing and Apache Iceberg table management, and Snowflake for querying and analytics.

