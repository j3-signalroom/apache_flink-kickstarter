/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.helper;

public class AvroSchemaFieldNotExistException extends Exception{
    public AvroSchemaFieldNotExistException(String errorMessage) {
        super(errorMessage);
    }
}
