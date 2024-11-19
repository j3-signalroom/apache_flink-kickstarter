/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter;

import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroSerializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.kafka.sink.*;
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


/**
 * This class creates fake flight data for two fictional airlines <b>Sunset Air</b> and 
 * <b>Sky One</b> Airlines</b>.  Then sinks the data to Kafka Topics and Apache Iceberg
 * Tables, respectively.
 */
public class AvroDataGeneratorApp {
    private static final Logger logger = LoggerFactory.getLogger(AvroDataGeneratorApp.class);


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
        // --- Retrieve the value(s) from the command line argument(s).
        String serviceAccountUser = Common.getAppArgumentValue(args, Common.ARG_SERVICE_ACCOUNT_USER);
        String awsRegion = Common.getAppArgumentValue(args, Common.ARG_AWS_REGION);

        // --- Create a configuration to force Avro serialization instead of Kyro serialization.
        org.apache.flink.configuration.Configuration config = new org.apache.flink.configuration.Configuration();
        config.set(PipelineOptions.FORCE_AVRO, true);

		// --- Create a blank Flink execution environment (a.k.a. the Flink job graph -- the DAG).
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(config);

        // --- Enable checkpointing every 5000 milliseconds (5 seconds).
        env.enableCheckpointing(5000);

        /*
         * Set timeout to 60 seconds
         * The maximum amount of time a checkpoint attempt can take before being discarded.
         */
        env.getCheckpointConfig().setCheckpointTimeout(60000);

        /*
         * Set the maximum number of concurrent checkpoints to 1 (i.e., only one checkpoint
         * is created at a time).
         */
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);

        // --- Create a StreamTableEnvironment
        EnvironmentSettings settings = 
            EnvironmentSettings.newInstance()
                               .inStreamingMode()
                               .build();
        StreamTableEnvironment tblEnv = StreamTableEnvironment.create(env, settings);

		/*
		 * --- Kafka Producer Config
		 * Retrieve the properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
		 * Then ingest properties into the Flink app's data stream.
		 */
        DataStream<Properties> dataStreamProducerProperties = 
			env.fromData(new Properties())
			   .map(new ConfluentClientConfigurationMapFunction(false, serviceAccountUser))
			   .name("kafka_producer_properties");
		Properties producerProperties = new Properties();

		/*
		 * Execute the data stream and collect the properties.
		 * 
		 * Note, the try-with-resources block ensures that the close() method of the CloseableIterator is
		 * called automatically at the end, even if an exception occurs during iteration.
		 */
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

        // --- Retrieve the schema registry URL from the producer properties.
        final String schemaRegistryUrl = producerProperties.getProperty("schema.registry.url");
        Map<String, String> registryConfigs = new HashMap<String, String>();
        registryConfigs.put("schema.registry.url", schemaRegistryUrl);
        registryConfigs.put("schema.registry.basic.auth.credentials.source", producerProperties.getProperty("schema.registry.basic.auth.credentials.source"));
        registryConfigs.put("schema.registry.basic.auth.user.info", producerProperties.getProperty("schema.registry.basic.auth.user.info"));

        // --- Create a data generator source.
        DataGeneratorSource<AirlineAvroData> skyOneSource =
            new DataGeneratorSource<>(
                index -> DataGenerator.generateAirlineAvroData("SKY1"),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(1),
                Types.POJO(AirlineAvroData.class)
            );

        // --- Sets up a Flink Avro Generic Record source to consume data.
        DataStream<AirlineAvroData> skyOneStream = 
            env.fromSource(skyOneSource, WatermarkStrategy.noWatermarks(), "skyone_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.skyone_avro` with the
         * specified serializer.
         */
        KafkaRecordSerializationSchema<AirlineAvroData> skyOneSerializer = 
            KafkaRecordSerializationSchema.<AirlineAvroData>builder()
                .setTopic("airline.skyone_avro")
                .setValueSerializationSchema(ConfluentRegistryAvroSerializationSchema.forSpecific(AirlineAvroData.class, AirlineAvroData.SUBJECT, schemaRegistryUrl, registryConfigs))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG).
         */
        KafkaSink<AirlineAvroData> skyOneSink = 
            KafkaSink.<AirlineAvroData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(skyOneSerializer)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        /*
         * Adds the given Sink to the DAG. Note only streams with sinks added will be executed
         * once the StreamExecutionEnvironment.execute() method is called.
         */
        skyOneStream.sinkTo(skyOneSink).name("skyone_sink");

        // --- Sets up a Flink Avro Generic Record source to consume data.
        DataGeneratorSource<AirlineAvroData> sunsetSource =
            new DataGeneratorSource<>(
                index -> DataGenerator.generateAirlineAvroData("SUN"),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(1),
                Types.POJO(AirlineAvroData.class)
            );

        DataStream<AirlineAvroData> sunsetStream = 
            env.fromSource(sunsetSource, WatermarkStrategy.noWatermarks(), "sunset_source");

        /*
         * Sets up a Flink Kafka sink to produce data to the Kafka topic `airline.sunset_avro` with the
         * specified serializer.
         */
        KafkaRecordSerializationSchema<AirlineAvroData> sunsetSerializer = 
            KafkaRecordSerializationSchema.<AirlineAvroData>builder()
                .setTopic("airline.sunset_avro")
                .setValueSerializationSchema(ConfluentRegistryAvroSerializationSchema.forSpecific(AirlineAvroData.class, AirlineAvroData.SUBJECT, schemaRegistryUrl, registryConfigs))
                .build();

        /*
         * Takes the results of the Kafka sink and attaches the unbounded data stream to the Flink
         * environment (a.k.a. the Flink job graph -- the DAG).
         */
        KafkaSink<AirlineAvroData> sunsetSink = 
            KafkaSink.<AirlineAvroData>builder()
                .setKafkaProducerConfig(producerProperties)
                .setRecordSerializer(sunsetSerializer)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        /*
         * Adds the given Sink to the DAG. Note only streams with sinks added will be executed
         * once the StreamExecutionEnvironment.execute() method is called.
         */
        sunsetStream.sinkTo(sunsetSink).name("sunset_sink");

        // --- Describes and configures the catalog for the Table API and Flink SQL.
        String catalogName = "apache_kickstarter";
        String bucketName = serviceAccountUser.replace("_", "-");  // --- To follow S3 bucket naming convention, replace underscores with hyphens if exist in string.
        String catalogImpl = "org.apache.iceberg.aws.glue.GlueCatalog";
        String databaseName = "airlines";
        Map<String, String> catalogProperties = new HashMap<>();
        catalogProperties.put("type", "iceberg");
        catalogProperties.put("warehouse", "s3://" + bucketName + "/warehouse");
        catalogProperties.put("catalog-impl", catalogImpl);
        catalogProperties.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
        catalogProperties.put("glue.skip-archive", "true");
        catalogProperties.put("glue.region", awsRegion);
        CatalogDescriptor catalogDescriptor = CatalogDescriptor.of(catalogName, org.apache.flink.configuration.Configuration.fromMap(catalogProperties));

        // --- Create the catalog, use it, and get the instantiated catalog.
        tblEnv.createCatalog(catalogName, catalogDescriptor);
        tblEnv.useCatalog(catalogName);
        org.apache.flink.table.catalog.Catalog catalog = tblEnv.getCatalog(catalogName).orElseThrow(() -> new RuntimeException("Catalog not found"));

        // --- Print the current catalog name.
        System.out.println("Current catalog: " + tblEnv.getCurrentCatalog());

        // --- Check if the database exists.  If not, create it.
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

        // --- Print the current database name.
        System.out.println("Current database: " + tblEnv.getCurrentDatabase());

        // --- Define the RowType for the RowData.
        RowType rowType = RowType.of(
            new org.apache.flink.table.types.logical.LogicalType[] {
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
        
        // --- Use the CatalogLoader since AWS Glue Catalog is used as the external metastore.
        CatalogLoader catalogLoader = CatalogLoader.custom(catalogName, catalogProperties,  new Configuration(false), catalogImpl);

        // --- Sink the datastreams to their respective Apache Iceberg tables.
        SinkToIcebergTable(tblEnv, catalog, catalogLoader, databaseName, rowType.getFieldCount(), "skyone_airline", skyOneStream);
        SinkToIcebergTable(tblEnv, catalog, catalogLoader, databaseName, rowType.getFieldCount(), "sunset_airline", sunsetStream);

        // --- Execute the Flink job graph (DAG)
        try {            
            env.execute("DataGeneratorApp");
        } catch (Exception e) {
            logger.error("The App stopped early due to the following: {}", e.getMessage());
        }
	}

    /**
     * This method is used to sink the data from the input data stream into the iceberg table.
     * 
     * @param tblEnv The StreamTableEnvironment.
     * @param catalog The Catalog. 
     * @param catalogLoader The CatalogLoader.
     * @param databaseName  The name of the database.
     * @param fieldCount The number of fields in the input data stream.
     * @param tableName The name of the table. 
     * @param airlineDataStream The input data stream.
     */
    private static void SinkToIcebergTable(final StreamTableEnvironment tblEnv, final org.apache.flink.table.catalog.Catalog catalog, final CatalogLoader catalogLoader, final String databaseName, final int fieldCount, final String tableName, DataStream<AirlineAvroData> airlineDataStream) {
        // --- Convert DataStream<AirlineData> to DataStream<RowData>
        DataStream<RowData> skyOneRowData = airlineDataStream.map(new MapFunction<AirlineAvroData, RowData>() {
            @Override
            public RowData map(AirlineAvroData airlineData) throws Exception {
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
        
        TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);

        // --- Create the table if it does not exist.
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

        /*
         * Serializable loader to load an Apache Iceberg Table.  Apache Flink needs to get Table objects in the cluster,
         * not just on the client side. So we need an Iceberg table loader to get the Table object.
         */
        TableLoader tableLoader = TableLoader.fromCatalog(catalogLoader, tableIdentifier);

        /*
         * Writes data from the Apache Flink datastream to an Apache Iceberg table using upsert logic, where updates or insertions 
         * are decided based on the specified equality fields (i.e., "email_address", "departure_airport_code", "arrival_airport_code").
         */
        FlinkSink
            .forRowData(skyOneRowData)
            .tableLoader(tableLoader)
            .upsert(true)
            .equalityFieldColumns(Arrays.asList("email_address", "departure_airport_code", "arrival_airport_code"))
            .append();
    }
}
