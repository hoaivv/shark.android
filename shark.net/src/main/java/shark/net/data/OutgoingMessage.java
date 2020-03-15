package shark.net.data;

import java.io.OutputStream;

public abstract class OutgoingMessage extends Message {

    private OutputStream data;

    public OutputStream getData() {
        return data;
    }

    OutgoingMessage(long transactionId, int type, OutputStream data, Object state) {
        super(transactionId, type | Message.Outgoing, state);
        this.data = data;
    }
}
