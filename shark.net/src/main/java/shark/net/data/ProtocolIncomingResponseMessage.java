package shark.net.data;

import java.io.InputStream;

public final class ProtocolIncomingResponseMessage extends IncomingResponseMessage {
    public ProtocolIncomingResponseMessage(InputStream data, Object state) {
        super(0, Message.ProtocolMessage, data, state);
    }
}
