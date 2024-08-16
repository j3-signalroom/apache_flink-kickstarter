/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * Defined enumerators
 */
package apache_flink.kickstarter.enums;


import java.util.*;


public enum ErrorEnum {
    // --- The constructor is invoked by the enums below
    ERR_CODE_MISSING_OR_INVALID_FIELD("ERR_CODE_MISSING_OR_INVALID_FIELD", ""),
    ERR_CODE_IO_EXCEPTION("ERR_CODE_IO_EXCEPTION", ""),
    ERR_CODE_AWS_REQUESTED_SECRET_DOES_HAVE_NOT_STRING("ERR_CODE_AWS_REQUESTED_SECRET_DOES_HAVE_NOT_STRING", ""),
    ERR_CODE_AWS_REQUESTED_SECRET_HAS_NO_VALUE("ERR_CODE_AWS_REQUESTED_SECRET_HAS_NO_VALUE", ""),
    ERR_CODE_AWS_REQUESTED_SECRET_NOT_FOUND("ERR_CODE_AWS_REQUESTED_SECRET_NOT_FOUND", ""), 
    ERR_CODE_AWS_INVALID_REQUEST("ERR_CODE_AWS_INVALID_REQUEST", ""),
    ERR_CODE_AWS_INVALID_PARAMETERS("ERR_CODE_AWS_INVALID_PARAMETERS", ""),
    ERR_CODE_AWS_REQUESTED_PARAMETER_PREFIX_NOT_FOUND("ERR_CODE_AWS_REQUESTED_PARAMETER_PREFIX_NOT_FOUND", "");

    // --- Avoid iterating the enum values by using a Map to cache them
    private static final Map<String, ErrorEnum> _BY_ERROR_CODE = new HashMap<>();
    private static final Map<String, ErrorEnum> _BY_ERROR_MESSAGE = new HashMap<>();

    private String errorMessageCode;
    private String errorMessage;

    static {
        for (ErrorEnum errorEnum : values()) {
            _BY_ERROR_CODE.put(errorEnum.errorMessageCode, errorEnum);
            _BY_ERROR_MESSAGE.put(errorEnum.errorMessage, errorEnum);
        }
    }

    /**
     * Default constructor method.
     *
     * @param errorMessageCode
     * @param errorMessage
     */
    private ErrorEnum(String errorMessageCode, String errorMessage) {
        this.errorMessageCode = errorMessageCode;
        this.errorMessage = errorMessage;
    }

    /**
     * This method returns the enum value by the errorMessageCode.
     *
     * @param errorMessageCode
     * @return
     */
    public static ErrorEnum valueOferrorMessageCode(String errorMessageCode) {
        return _BY_ERROR_CODE.get(errorMessageCode);
    }

    /**
     * This method returns the enum value by the errorMessage.
     *
     * @param errorMessage
     * @return The enum value by the errorMessage.
     */
    public static ErrorEnum valueOfErrorMessage(String errorMessage) {
        return _BY_ERROR_MESSAGE.get(errorMessage);
    }

    /**
     * @return The error code.
     */
    public String getCode() {
        return this.errorMessageCode;
    }

     /**
     * @return The error message.
     */
    public String getMessage() {
        return this.errorMessage;
    }
}