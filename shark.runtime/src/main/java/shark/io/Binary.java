package shark.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class Binary {

    private static byte[] read(InputStream input, int length) throws IOException {

        int count = 0;
        byte[] buffer = new byte[length];

        while (count < length) count += input.read(buffer, count, length - count);

        return buffer;
    }

    public static byte readByte(InputStream input) throws IOException {

        return read(input, 1)[0];
    }

    public static int readInt(InputStream input) throws IOException {

        return ByteBuffer.wrap(read(input, Integer.BYTES)).getInt();
    }

    public static long readLong(InputStream input) throws IOException {

        return ByteBuffer.wrap(read(input, Long.BYTES)).getLong();
    }

    public static float readFloat(InputStream input) throws IOException {

        return ByteBuffer.wrap(read(input, Float.BYTES)).getFloat();
    }

    public static double readDouble(InputStream input) throws IOException {
        return ByteBuffer.wrap(read(input, Double.BYTES)).getDouble();
    }

    public static void write(OutputStream output, byte value) throws IOException {
        output.write(new byte[] { value });
    }

    public static void write(OutputStream output, int value) throws IOException {

        output.write(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    public static void write(OutputStream output, long value) throws IOException {

        output.write(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }

    public static void write(OutputStream output, float value) throws IOException {

        output.write(ByteBuffer.allocate(Float.BYTES).putFloat(value).array());
    }

    public static void write(OutputStream output, double value) throws IOException {

        output.write(ByteBuffer.allocate(Double.BYTES).putDouble(value).array());
    }
}
