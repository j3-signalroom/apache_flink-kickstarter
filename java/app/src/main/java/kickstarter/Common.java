/**
 * Copyright (c) 2024-2025 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class contains common methods that are used throughout the application.
 */
package kickstarter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.util.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Common {
    private static final Logger LOGGER = LoggerFactory.getLogger(Common.class);

    public static final String ARG_SERVICE_ACCOUNT_USER = "--service-account-user";
    public static final String ARG_AWS_REGION = "--aws-region";
    

    /**
     * @return returns a new instance of the Jackson ObjectMapper with the JavaTimeModule
     * registered.
     */
	public static ObjectMapper getMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule()).setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
	}

    /**
     * This method Loops through the `args` parameter and checks for the argument.
     * 
     * @param args list of strings passed to the main method.
     * @param argument the argument to check for.
     * @return true if the flag is found, false otherwise.
     */
    public static String getAppArgumentValue(final String[] args, final String argument) {
        String serviceAccountUser = "";
        
        // --- Loop through the args parameter and check for the argument
        Iterator <String> iterator = List.of(args).iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
			if(arg.equalsIgnoreCase(argument)) {
                if(iterator.hasNext()) {
                    serviceAccountUser = iterator.next();
                }
            }
		}
        return serviceAccountUser;
    }

    /**
     * This method checks if a catalog exists in the TableEnvironment.
     * 
     * @param tblEnv the TableEnvironment instance.
     * @param catalogName the name of the catalog to check.
     * @return true if the catalog exists, false otherwise.
     */
    public static boolean isCatalogExist(final TableEnvironment tblEnv, final String catalogName) {
        // Check if the catalog exists
        Catalog catalog = null;
        try {
            catalog = tblEnv.getCatalog(catalogName).orElse(null);
        } catch (Exception e) {
            LOGGER.error( "Error while checking catalog existence: {}", e.getMessage(), e);
        }

        return catalog != null;
    }

    /**
     * This method collects the Confluent Kafka properties.  Moreover, because it is a static 
     * method it is called without creating an instance of the class.  In effect caching the
     * properties for the application.
     * 
     * @param env
     * @param serviceAccountUser
     * @param isConsumer
     * @return
     * @throws Exception
     */
    public static Properties collectConfluentProperties(StreamExecutionEnvironment env, String serviceAccountUser, boolean isConsumer) throws Exception {
        DataStream<Properties> dataStreamProperties =    
            env.fromData(new Properties())
                .map(new ConfluentClientConfigurationMapFunction(isConsumer, serviceAccountUser))
                .name(isConsumer ? "consumer_properties" : "producer_properties");

        Properties properties = new Properties();

        try (CloseableIterator<Properties> iterator = dataStreamProperties.executeAndCollect()) {
            iterator.forEachRemaining(properties::putAll);
        } catch (Exception e) {
            LOGGER.error("Error collecting Kafka properties: ", e);
            throw new RuntimeException("Failed to collect Kafka properties", e);
        }
        return properties;
    }

    /**
     * This method extracts the registry configurations from the properties.
     * 
     * @param properties the properties to extract the registry configurations from.
     * 
     * @return a map of the registry configurations.
     */
    public static Map<String, String> extractRegistryConfigs(Properties properties) {
        return properties
                    .stringPropertyNames()
                    .stream()
                    .filter(key -> key.startsWith("schema.registry."))
                    .collect(Collectors.toMap(key -> key, properties::getProperty));
    }
}
