/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package apache_flink.kickstarter;

/**
 * This class contains the options passed in the main methods of the app classes.
 */
public class AppOptions {
    boolean getFromAws;
    String serviceAccountUser;


    /**
     * Constructor for the AppOptions class.
     * 
     * @param getFromAws
     * @param serviceAccountUser
     */
    public AppOptions(boolean getFromAws, String serviceAccountUser) {
        this.getFromAws = getFromAws;
        this.serviceAccountUser = serviceAccountUser;
    }

    /**
     * @return the value of the getFromAws field.
     */
    public boolean isGetFromAws() {
        return getFromAws;
    }

    /**
     * @return the value of the serviceAccountUser field.
     */
    public String getServiceAccountUser() {
        return serviceAccountUser;
    }
}
