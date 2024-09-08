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
     * Default constructor.
     */
    public AppOptions() {}

    /**
     * Constructor when the the fields are set on instantiation.
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

    /**
     * Sets the value of the getFromAws field.
     * 
     * @param getFromAws the value to set the getFromAws field to.
     */
    public void setGetFromAws(boolean getFromAws) {
        this.getFromAws = getFromAws;
    }

    /**
     * Sets the value of the serviceAccountUser field.
     * 
     * @param serviceAccountUser the value to set the serviceAccountUser field to.
     */
    public void setServiceAccountUser(String serviceAccountUser) {
        this.serviceAccountUser = serviceAccountUser;
    }
}
