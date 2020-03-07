package shark.runtime.serialization;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Json Serialier of Shark Framework
 */
public class JsonSerializer extends Serializer {

    private static Gson gson = new Gson();

    /**
     * Serializes an object and write the serialized data to a stream. This method to be used if
     * the stream should contain only one object.
     * @param output Stream to which serialized data of the object to be written
     * @param data Object to be serialized
     * @throws SerializationException throws if object could not be serialized to the stream
     */
    @Override
    public void serialize(OutputStream output, Object data) throws SerializationException {

        try {

            if (data == null) {
                output.write("null".getBytes());
            }
            else {
                output.write(gson.toJson(data).getBytes());
            }
        }
        catch (Exception e) {
            throw new SerializationException("failed to serialize", e);
        }
    }

    /**
     * Serializes an object and write the serialized data to a stream with length prefix. This
     * method to be used if the stream should contain more than one object.
     * @param output Stream to which serialized data of the object to be written
     * @param data Object to be serialized
     * @throws SerializationException throws if object could not be serialized to the stream
     */
    @Override
    public void serializeWithLengthPrefix(OutputStream output, Object data) throws SerializationException {

        try {

            if (data == null) {
                output.write("4.null".getBytes());
            }
            else {
                byte[] buffer = gson.toJson(data).getBytes();

                output.write((buffer.length + ".").getBytes());
                output.write(buffer);
            }
        }
        catch (Exception e) {
            throw new SerializationException("failed to serialize", e);
        }
    }

    /**
     * Reads serialized data from a stream and converts it to an object of a specified type. This
     * method to be used if the stream only contains one object.
     * @param input Stream contains serialized data
     * @param type class of the extracting object
     * @param <T> type of the extracting object
     * @return an instance of {@link T}
     * @throws SerializationException throws if object could not be extracted from the stream
     */
    @Override
    public <T> T deserialize(InputStream input, Class<T> type) throws SerializationException {

        try {
            return gson.fromJson(new InputStreamReader(input), type);
        }
        catch (Exception e) {
            throw new SerializationException("failed to deserialize", e);
        }
    }

    /**
     * Reads serialized data with length prefix from a stream and converts it to an object of a
     * specified type. This method to be used if the stream contains more than one object.
     * @param input Stream contains serialized data
     * @param type class of the extracting object
     * @param <T> type of the extracting object
     * @return an instance of {@link T}
     * @throws SerializationException throws if object could not be extracted from the stream
     */
    @Override
    public <T> T deserializeWithLengthPrefix(InputStream input, Class<T> type) throws SerializationException {

        try {

            String sPrefix = "";

            while (true) {
                int one = input.read();

                if (one < 0 || (char) one == '.') break;
                sPrefix += (char) one;
            }

            int length = Integer.parseInt(sPrefix);
            byte[] buffer = new byte[length];

            int count = 0;
            while (count < length) count += input.read(buffer, count, length - count);

            InputStreamReader reader = null;

            try {
                reader = new InputStreamReader(new ByteArrayInputStream(buffer));

                return gson.fromJson(reader, type);
            }
            finally {

                if (reader != null) reader.close();
                buffer = null;
            }
        }
        catch (Exception e) {
            throw new SerializationException("failed to deserialize", e);
        }
    }
}
