/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * Generic class object used to return an object, error message or both, and a success 
 * indicator back from a method call.
 */
package apache_flink.helper;


public class ObjectResult<T> {
    private boolean successful;
    private String errorMessageCode;
    private String errorMessage;
    private T t;

    /**
     * This is the default constructor method for when the object is successfully created.
     * 
     * @param t
     */
    public ObjectResult(T t) {
        this.successful = true;
        this.errorMessageCode = "";
        this.errorMessage = "";
        this.t = t;
    }

    /**
     * This is the default constructor method for when the object is NOT successfully created.
     *
     * @param errorMessageCode
     * @param errorMessage
     */
    public ObjectResult(String errorMessageCode, String errorMessage) {
        this.successful = false;
        this.errorMessageCode = errorMessageCode;
        this.errorMessage = errorMessage;
        this.t = null;
    }

    /**
     * This is the default constructor method for when the object is NOT successfully created.
     * However, there is a object that contains a special error information.
     *
     * @param errorMessageCode
     * @param errorMessage
     * @param t
     */
    public ObjectResult(String errorMessageCode, String errorMessage, T t) {
        this.successful = false;
        this.errorMessageCode = errorMessageCode;
        this.errorMessage = errorMessage;
        this.t = t;
    }

    /**
     * This is the default constructor method for when the object is successfully created, but
     * is not properly formed.
     *
     * @param t
     * @param errorMessage
     */
    public ObjectResult(String errorMessage, T t) {
        this.successful = false;
        this.errorMessageCode = "<UNDEFINED>";
        this.errorMessage = errorMessage;
        this.t = t;
    }

    /**
     * This is the default constructor method for copying one ObjectResult into another.
     *
     * @param o
     */
    public ObjectResult(ObjectResult<T> o) {
        this.successful = o.successful;
        this.errorMessageCode = o.errorMessageCode;
        this.errorMessage = o.getErrorMessage();
        this.t = o.get();
    }

    /**
     * This getter returns the success indicator of the function call.
     *
     * @return <b>true</b>, if the function call was successful; otherwise, <b>false</b> is returned.
     */
    public boolean isSuccessful() {
        return this.successful;
    }

    /**
     * This getter returns the error message code if the method errored out.
     *
     * @return This getter returns the error message code if the method errored out.
     */
    public String getErrorMessageCode() {
        return this.errorMessageCode;
    }

    /**
     * This getter returns the error message if the method errored out.
     *
     * @return This getter returns the error message if the method errored out.
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * This getter returns the <b>T</b> object.
     *
     * @return The <b>T</b> object.
     */
    public T get() {
        return this.t;
    }
}