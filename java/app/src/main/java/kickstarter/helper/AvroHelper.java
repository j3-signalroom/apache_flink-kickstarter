/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.helper;

import java.io.*;
import java.util.*;
import javax.xml.bind.*;
import io.confluent.kafka.schemaregistry.avro.*;
import io.confluent.kafka.schemaregistry.client.*;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.*;
import tech.allegro.schema.json2avro.converter.*;
import org.apache.avro.*;
import org.apache.avro.generic.*;
import org.apache.avro.io.*;
import org.json.*;


public class AvroHelper {
    // --- Set the maximum number of schema objects that can be cached for a schema
    private static final int SCHEMA_MAX_CACHE_CAPACITY = 1000;
    private static final String SCHEMA_REGISTRY_URL_PROPERTY_NAME = "schema.registry.url";


    private AvroHelper() {}


    /**
     * The method returns the schema version of the Kafka topic stored in the schema registry.
     *
     * @param properties
     * @param schemaId
     * @return The <b>ObjectResult{@literal <}Schema{@literal >}</b> class object is returned.
     */
    public static Schema getSchema(final Map<String, String> properties, final int schemaId) throws RestClientException, IOException {
        try(SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(Collections.singletonList(properties.get(SCHEMA_REGISTRY_URL_PROPERTY_NAME)), SCHEMA_MAX_CACHE_CAPACITY, Collections.singletonList(new AvroSchemaProvider()), properties)) {
            final Schema.Parser parsedSchema = new Schema.Parser();
            return parsedSchema.parse(schemaRegistryClient.getSchemaById(schemaId).rawSchema().toString());
        }
    }

    /**
     * 
     *
     * @param properties
     * @param kafkaSchemaVersionId
     * @param kafkaEmbeddedSchemaName
     * @return
     * @throws RestClientException
     * @throws IOException
     */
    public static Map<String, Schema> getSchemaSet(final Map<String, String> properties, final int kafkaSchemaVersionId, final String kafkaEmbeddedSchemaName) throws RestClientException, IOException {
        try(SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(Collections.singletonList(properties.get(SCHEMA_REGISTRY_URL_PROPERTY_NAME)), SCHEMA_MAX_CACHE_CAPACITY, Collections.singletonList(new AvroSchemaProvider()), properties)) {
            final Schema.Parser parsedSchema = new Schema.Parser();
            parsedSchema.parse(schemaRegistryClient.getSchemaBySubjectAndId(kafkaEmbeddedSchemaName, kafkaSchemaVersionId).rawSchema().toString());
            return parsedSchema.getTypes();
        }
    }

    /**
     * This method gets the embedded schema of the host schema.
     *
     * @param properties
     * @param kafkaSubjectSchemaVersionId
     * @param kafkaEmbeddedSchemaName
     * @return
     * @throws RestClientException
     * @throws IOException
     */
    public static Schema getSchema(final Map<String, String> properties, final int kafkaSubjectSchemaVersionId, final String kafkaEmbeddedSchemaName) throws RestClientException, IOException {
        try(SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(Collections.singletonList(properties.get(SCHEMA_REGISTRY_URL_PROPERTY_NAME)), SCHEMA_MAX_CACHE_CAPACITY, Collections.singletonList(new AvroSchemaProvider()), properties)) {
            final Schema.Parser parsedSchema = new Schema.Parser();
            parsedSchema.parse(schemaRegistryClient.getSchemaById(kafkaSubjectSchemaVersionId).rawSchema().toString());
            return parsedSchema.getTypes().get(kafkaEmbeddedSchemaName);
        }
    }

    /**
     * This method returns the schema field name of the alias, which is key in <b>figuring out if
     * an Avro schema has evolved.</b>
     *
     * @param schema
     * @param aliasName
     * @return The field name of the alias.
     */
    private static String getAvroFieldNameFromAliasName(final Schema schema, final String aliasName) {
        for(int fieldIndex = 0; fieldIndex < schema.getFields().size(); fieldIndex++)
            for(int aliasIndex = 0; aliasIndex < schema.getFields().get(fieldIndex).aliases().size(); aliasIndex++)
                if(schema.getFields().get(fieldIndex).aliases().contains(aliasName))
                    return schema.getFields().get(fieldIndex).name();
        return null;
    }

    /**
     * The method would return <b>true</b> if the put-call succeed.  Otherwise, <b>false</b>
     * is returned.  The key to this method is that it retries after it initially throws an
     * exception, checks to see if it was because the schema field was not valid.  Then checks
     * if the field first used is an alias and attempts to its field name.  <b>If it can, it
     * retries with the alias field name instead.</b>
     *
     * @param genericRecord
     * @param schema
     * @param fieldName
     * @param value
     */
    public static void setRecordField(final GenericRecord genericRecord, final Schema schema, String fieldName, final Object value) throws AvroSchemaFieldNotExistException {
        String errorMessage = setRecord(genericRecord, fieldName, value);

        try{
            if(!errorMessage.isEmpty() && errorMessage.contains("Not a valid schema field: ")) {
                // --- The field maybe an alias, so in that case.  Check again using the field name of the alias
                fieldName = getAvroFieldNameFromAliasName(schema, errorMessage.substring(errorMessage.lastIndexOf(' ') + 1));

                if(fieldName != null) {
                    errorMessage = setRecord(genericRecord, fieldName, value);

                    if(errorMessage.isEmpty())
                        /*
                        * Yes, it was an alias field after all, the schema had evolved since the last time the code
                        * was built.  However, the code was able to self-heal, and set the record value without error to
                        * current field being used in the schema.
                        */
                        return;
                }
                throw new AvroSchemaFieldNotExistException(String.format("Value is '%s' being applied to schema '%s', because %s", fieldName, schema.getFullName(), errorMessage));
            }
        } catch(final NullPointerException e) {
            // Do nothing
        }        
    }

    /**
     * The method would return <b>true</b> if the put-call succeed.  Otherwise, <b>false</b>
     * is returned.
     *
     * @param genericRecord
     * @param fieldName
     * @param value
     * @return <b>true</b> if the field value was recorded in the GenericRecord object.
     */
    private static String setRecord(final GenericRecord genericRecord, String fieldName, final Object value) {
        try {
            /*
             * Note, it's important that the non-string values are casted.  Otherwise, an error will 
             * be generated when the code attempts to serialize the topic with the schema at runtime.
             */
            if(value.getClass() == Integer.class)
                genericRecord.put(fieldName, (int)value);
            else 
                if(value.getClass() == Long.class)
                    genericRecord.put(fieldName, (long)value);
                else 
                    if(value.getClass() == Float.class)
                        genericRecord.put(fieldName, (float)value);     
                    else
                        if(value.getClass() == Boolean.class)
                            genericRecord.put(fieldName, (boolean)value);
                        else
                            genericRecord.put(fieldName, value);

            return "";
        } catch (final Exception e) {
            return e.getMessage();
        }        
    }
    
    /**
     * The method convert a <b>GenericRecord</b> into a <b>JSONObject</b>.
     *
     * @param genericRecord
     * @return The converted <b>GenericRecord</b> as a <b>JSONObject</b>.
     */
    public static JSONObject toJson(GenericRecord genericRecord) {
        try {
            // --- Converts the Avro GenericRecord to binary JSON stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            NoWrappingJsonEncoder jsonEncoder = new NoWrappingJsonEncoder(genericRecord.getSchema(), outputStream);

            // --- Converts the Java object GenericRecord into in-memory serialized format of the schema
            DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(genericRecord.getSchema());
            writer.write(genericRecord, jsonEncoder);
            jsonEncoder.flush();

            // --- Decode Base64 JSON
            String base64Encoded = DatatypeConverter.printBase64Binary(outputStream.toByteArray());
            byte[] base64Decoded = DatatypeConverter.parseBase64Binary(base64Encoded);
            return new JSONObject(new String(base64Decoded));
        } catch(final IOException e) {
            return null;
        }
    }

    /**
     * The method returns the <b>GenericRecord</b> version of the JSONObject.
     *
     * @param schema
     * @param json
     * @return The <b>GenericRecord</b> version of the JSONObject.
     */
    public static GenericRecord toGenericRecord(final Schema schema, final JSONObject json) {
        JsonAvroConverter converter = new JsonAvroConverter();

        // --- Return the converted JSON
        return converter.convertToGenericDataRecord(json.toString().getBytes(), schema);
    }
}
