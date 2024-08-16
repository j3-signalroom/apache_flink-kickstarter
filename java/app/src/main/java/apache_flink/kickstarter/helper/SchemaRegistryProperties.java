/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.kickstarter.helper;

import java.util.*;


public class SchemaRegistryProperties {
    Map<String, String> schemaRegistryProperties;
    
    
    /**
     * The default constructor stores the parameter value(s) that are passed to it.
     * 
     * @param schemaRegistryProperties
     */
    public SchemaRegistryProperties(final Map<String, String> schemaRegistryProperties) {
        this.schemaRegistryProperties = schemaRegistryProperties;
    }

    /**
     * This getter returns the hashmap object of the Schema Registry properties.
     *
     * @return The hashmap object.
     */
    public Map<String, String> getSchemaRegistryProperties() {
        return this.schemaRegistryProperties;
    }
}
