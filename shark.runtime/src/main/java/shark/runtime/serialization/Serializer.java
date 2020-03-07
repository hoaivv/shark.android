package shark.runtime.serialization;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Base class of serializers used by Shark Framework components
 */
public abstract class Serializer {

    private static Serializer _default = new JsonSerializer();

    /**
     * Gets the default serializer used by Shark Framework
     * @return
     */
    public static Serializer getDefault() {
        return _default;
    }

    /**
     * Sets a serializer as the default serializer of Shark Framework
     * @param serializer
     */
    public static void setDefault(Serializer serializer) {
        if (serializer != null) _default = serializer;
    }

    /**
     * Serializes an object and write the serialized data to a stream. This method to be used if
     * the stream should contain only one object.
     * @param output Stream to which serialized data of the object to be written
     * @param data Object to be serialized
     * @throws SerializationException throws if object could not be serialized to the stream
     */
    public abstract void serialize(OutputStream output, Object data) throws SerializationException;

    /**
     * Serializes an object and write the serialized data to a stream with length prefix. This
     * method to be used if the stream should contain more than one object.
     * @param output Stream to which serialized data of the object to be written
     * @param data Object to be serialized
     * @throws SerializationException throws if object could not be serialized to the stream
     */
    public abstract void serializeWithLengthPrefix(OutputStream output, Object data) throws SerializationException;

    /**
     * Reads serialized data from a stream and converts it to an object of a specified type. This
     * method to be used if the stream only contains one object.
     * @param input Stream contains serialized data
     * @param type class of the extracting object
     * @param <T> type of the extracting object
     * @return an instance of {@link T}
     * @throws SerializationException throws if object could not be extracted from the stream
     */
    public abstract <T> T deserialize(InputStream input, Class<T> type) throws SerializationException;

    /**
     * Reads serialized data with length prefix from a stream and converts it to an object of a
     * specified type. This method to be used if the stream contains more than one object.
     * @param input Stream contains serialized data
     * @param type class of the extracting object
     * @param <T> type of the extracting object
     * @return an instance of {@link T}
     * @throws SerializationException throws if object could not be extracted from the stream
     */
    public abstract <T> T deserializeWithLengthPrefix(InputStream input, Class<T> type) throws SerializationException;
}
