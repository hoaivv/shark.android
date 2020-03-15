package shark.net.data;

import java.io.InputStream;

public abstract class IncomingMessage extends Message {

    private final InputStream data;

    public InputStream getData() {
        return data;
    }

    IncomingMessage(long transactionId, int type, InputStream data, Object state) {
        super(transactionId, type | Message.Incoming, state);
        this.data = data;
    }
}
