package shark.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Provides primitive data read/write operation
 */
public final class primitive {

    /**
     * Reads a specified number of bytes from a stream
     * @param input stream from which data to be read
     * @param length number of bytes to be read
     * @return an array contains read bytes
     * @throws IOException throws if the reading operation is failed
     */
    private static byte[] readBytes(InputStream input, int length) throws IOException {

        int count = 0;
        byte[] buffer = new byte[length];

        while (count < length) count += input.read(buffer, count, length - count);

        return buffer;
    }

    /**
     * Reads a byte from a stream
     * @param input stream from which data to be read
     * @return read byte
     * @throws IOException throws if the reading operation is failed
     */
    public static byte readByte(InputStream input) throws IOException {

        return readBytes(input, 1)[0];
    }

    /**
     * Reads an {@link Integer} value from a stream
     * @param input stream from which data to be read
     * @return read integer value
     * @throws IOException throws if the reading operation is failed
     */
    public static int readInt(InputStream input) throws IOException {

        return ByteBuffer.wrap(readBytes(input, Integer.BYTES)).getInt();
    }

    /**
     * Reads a {@link Long} value from a stream
     * @param input stream from which data to be read
     * @return read long value
     * @throws IOException throws if the reading operation is failed
     */
    public static long readLong(InputStream input) throws IOException {

        return ByteBuffer.wrap(readBytes(input, Long.BYTES)).getLong();
    }

    /**
     * Reads a {@link Float} value from a stream
     * @param input stream from which data to be read
     * @return read float value
     * @throws IOException throws if the reading operation is failed
     */
    public static float readFloat(InputStream input) throws IOException {

        return ByteBuffer.wrap(readBytes(input, Float.BYTES)).getFloat();
    }

    /**
     * Reads a {@link Double} value from a stream
     * @param input stream from which data to be read
     * @return read double value
     * @throws IOException throws if the reading operation is failed
     */
    public static double readDouble(InputStream input) throws IOException {
        return ByteBuffer.wrap(readBytes(input, Double.BYTES)).getDouble();
    }

    /**
     * Writes a byte to a stream
     * @param output stream to which data to be written
     * @param value value to be written
     * @throws IOException throws if the writing operation failed
     */
    public static void write(OutputStream output, byte value) throws IOException {
        output.write(new byte[] { value });
    }

    /**
     * Writes an {@link Integer} value to a stream
     * @param output stream to which data to be written
     * @param value value to be written
     * @throws IOException throws if the writing operation failed
     */
    public static void write(OutputStream output, int value) throws IOException {

        output.write(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    /**
     * Writes a {@link Long} value to a stream
     * @param output stream to which data to be written
     * @param value value to be written
     * @throws IOException throws if the writing operation failed
     */
    public static void write(OutputStream output, long value) throws IOException {

        output.write(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }

    /**
     * Writes a {@link Float} value to a stream
     * @param output stream to which data to be written
     * @param value value to be written
     * @throws IOException throws if the writing operation failed
     */
    public static void write(OutputStream output, float value) throws IOException {

        output.write(ByteBuffer.allocate(Float.BYTES).putFloat(value).array());
    }

    /**
     * Writes a {@link Double} value to a stream
     * @param output stream to which data to be written
     * @param value value to be written
     * @throws IOException throws if the writing operation failed
     */
    public static void write(OutputStream output, double value) throws IOException {

        output.write(ByteBuffer.allocate(Double.BYTES).putDouble(value).array());
    }
}
