package shark.runtime.serialization;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class Serializer {

    private static Serializer _default = new JsonSerializer();

    public static Serializer getDefault() {
        return _default;
    }

    public static void setDefault(Serializer serializer) {
        if (serializer != null) _default = serializer;
    }

    public abstract void serialize(OutputStream output, Object data) throws SerializationException;
    public abstract void serializeWithLengthPrefix(OutputStream output, Object data) throws SerializationException;

    public abstract <T> T deserialize(InputStream input, Class<T> type) throws SerializationException;
    public abstract <T> T deserializeWithLengthPrefix(InputStream input, Class<T> type) throws SerializationException;
}
