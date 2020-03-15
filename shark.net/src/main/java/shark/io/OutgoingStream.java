package shark.io;

import java.io.IOException;
import java.io.OutputStream;

public class OutgoingStream extends OutputStream {

    private final OutputStream base;
    private long lastActive = System.currentTimeMillis();

    public long getLastActive() {
        return lastActive;
    }

    public OutgoingStream(OutputStream base) {
        this.base = base;
    }

    @Override
    public void close() throws IOException {
        base.close();
    }

    @Override
    public void write(byte[] b) throws IOException {
        base.write(b);
        lastActive = System.currentTimeMillis();
    }

    @Override
    public void flush() throws IOException {
        base.flush();
        lastActive = System.currentTimeMillis();
    }

    @Override
    public void write(int b) throws IOException {
        base.write(b);
        lastActive = System.currentTimeMillis();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        base.write(b, off, len);
        lastActive = System.currentTimeMillis();
    }
}
