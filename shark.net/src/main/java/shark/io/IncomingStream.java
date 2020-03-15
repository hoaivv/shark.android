package shark.io;

import java.io.IOException;
import java.io.InputStream;

public class IncomingStream extends InputStream {

    private final InputStream base;
    private long lastActive = System.currentTimeMillis();

    public long getLastActive(){
        return lastActive;
    }

    public IncomingStream(InputStream base) {
        this.base = base;
    }

    @Override
    public int read() throws IOException {
        int result = this.base.read();
        lastActive = System.currentTimeMillis();
        return result;
    }

    @Override
    public void close() throws IOException {
        base.close();
    }

    @Override
    public synchronized void reset() throws IOException {
        base.reset();
        lastActive = System.currentTimeMillis();
    }

    @Override
    public synchronized void mark(int readlimit) {
        base.mark(readlimit);
    }

    @Override
    public long skip(long n) throws IOException {
        long result = base.skip(n);
        if (result > 0) lastActive = System.currentTimeMillis();
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result =  base.read(b, off, len);
        if (result > 0) lastActive = System.currentTimeMillis();
        return result;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result = base.read(b);
        if (result > 0) lastActive = System.currentTimeMillis();
        return result;
    }

    @Override
    public boolean markSupported() {
        return base.markSupported();
    }

    @Override
    public int available() throws IOException {
        return base.available();
    }
}
