/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * This class contains common methods that are used throughout the application.
 */
package apache_flink.kickstarter;

import java.util.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


public class Common {
    private static final String FLAG_GET_FROM_AWS = "--get-from-aws";
    private static final String FLAG_SERVICE_ACCOUNT_USER = "--service-account-user";


    /**
     * This method Loops through the `args` parameter and checks for the`FLAG_GET_FROM_AWS` flag.
     * 
     * @param args list of strings passed to the main method.
     * @return true if the flag is found, false otherwise.
     */
    public static boolean checkForFlagGetFromAws(final String[] args) {
        Iterator <String> iterator = List.of(args).iterator();
        
        while (iterator.hasNext()) {
            String arg = iterator.next();
			if(arg.equalsIgnoreCase(FLAG_GET_FROM_AWS))
                return true;
            if(arg.equalsIgnoreCase(FLAG_SERVICE_ACCOUNT_USER)) {

            }
		}
        return false;
    }

    /**
     * @return returns a new instance of the Jackson ObjectMapper with the JavaTimeModule
     * registered.
     */
	public static ObjectMapper getMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}
