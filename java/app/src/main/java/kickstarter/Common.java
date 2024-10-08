/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class contains common methods that are used throughout the application.
 */
package kickstarter;

import java.util.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.catalog.Catalog;


public class Common {
    private static final String OPT_SERVICE_ACCOUNT_USER = "--service-account-user";


    /**
     * This method Loops through the `args` parameter and checks for the `OPT_SERVICE_ACCOUNT_USER`
     * option.
     * 
     * @param args list of strings passed to the main method.
     * @return true if the flag is found, false otherwise.
     */
    public static String getAppOptions(final String[] args) {
        String serviceAccountUser = "";
        
        // --- Loop through the args parameter and check for the `OPT_SERVICE_ACCOUNT_USER` option
        Iterator <String> iterator = List.of(args).iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
			if(arg.equalsIgnoreCase(OPT_SERVICE_ACCOUNT_USER)) {
                if(iterator.hasNext()) {
                    serviceAccountUser = iterator.next();
                }
            }
		}
        return serviceAccountUser;
    }

    /**
     * @return returns a new instance of the Jackson ObjectMapper with the JavaTimeModule
     * registered.
     */
	public static ObjectMapper getMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
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
            System.err.println("Error while checking catalog existence: " + e.getMessage());
        }

        return (catalog != null) ? true : false;
    }
}
