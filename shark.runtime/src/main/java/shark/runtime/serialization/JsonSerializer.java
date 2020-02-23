package shark.runtime.serialization;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class JsonSerializer extends Serializer {

    private static Gson gson = new Gson();

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

    @Override
    public <T> T deserialize(InputStream input, Class<T> type) throws SerializationException {

        try {
            return gson.fromJson(new InputStreamReader(input), type);
        }
        catch (Exception e) {
            throw new SerializationException("failed to deserialize", e);
        }
    }

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
