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
    private static final String OPT_GET_FROM_AWS = "--get-from-aws";
    private static final String OPT_SERVICE_ACCOUNT_USER = "--service-account-user";


    /**
     * This method Loops through the `args` parameter and checks for the`OPT_GET_FROM_AWS` and `OPT_SERVICE_ACCOUNT_USER` options.
     * 
     * @param args list of strings passed to the main method.
     * @return true if the flag is found, false otherwise.
     */
    public static AppOptions getAppOptions(final String[] args) {
        AppOptions appOptions = new AppOptions(false, "");
        Iterator <String> iterator = List.of(args).iterator();
        
        while (iterator.hasNext()) {
            String arg = iterator.next();
			if(arg.equalsIgnoreCase(OPT_GET_FROM_AWS))
                appOptions.setGetFromAws(true);
            else if(arg.equalsIgnoreCase(OPT_SERVICE_ACCOUNT_USER)) {
                if(iterator.hasNext()) {
                    appOptions.setServiceAccountUser(iterator.next());
                }
            }
		}

        return appOptions;
    }

    /**
     * @return returns a new instance of the Jackson ObjectMapper with the JavaTimeModule
     * registered.
     */
	public static ObjectMapper getMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}
