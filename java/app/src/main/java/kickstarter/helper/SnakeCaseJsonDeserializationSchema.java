/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 */
package kickstarter.helper;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;


/**
 * A deserialization schema that deserializes a JSON object into an object of the target type.
 * The JSON object is expected to be in snake_case format.
 * 
 * @param <T> The type of the deserialized object.
 */
public class SnakeCaseJsonDeserializationSchema<T> implements DeserializationSchema<T> {
    private final Class<T> targetType;
    private final ObjectMapper objectMapper;

    /**
     * Constructor.  Sets the target type of the deserialized object.
     * 
     * @param targetType The type of the deserialized object.
     */
    public SnakeCaseJsonDeserializationSchema(Class<T> targetType) {
        this.targetType = targetType;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * Deserializes the message into an object of the target type.
     * 
     * @param message The message to be deserialized.
     * 
     * @return The deserialized object.
     */
    @Override
    public T deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, targetType);
    }

    /**
     * Method to decide whether the element signals the end of the stream. If true is
     * returned the element won't be emitted.
     * 
     * @param nextElement The element to test for the end-of-stream signal.
     * 
     * @return True, if the element signals end of stream, false otherwise.
     */
    @Override
    public boolean isEndOfStream(T nextElement) {
        return false;
    }

    /**
     * Gets the type information of the produced type.
     * 
     * @return The type information.
     */
    @Override
    public TypeInformation<T> getProducedType() {
        return TypeInformation.of(targetType);
    }
}