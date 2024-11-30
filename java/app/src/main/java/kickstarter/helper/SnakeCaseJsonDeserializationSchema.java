package kickstarter.helper;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.IOException;

public class SnakeCaseJsonDeserializationSchema<T> implements DeserializationSchema<T> {
    private final Class<T> targetType;
    private final ObjectMapper objectMapper;

    public SnakeCaseJsonDeserializationSchema(Class<T> targetType) {
        this.targetType = targetType;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Override
    public T deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, targetType);
    }

    @Override
    public boolean isEndOfStream(T nextElement) {
        return false;
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return TypeInformation.of(targetType);
    }
}