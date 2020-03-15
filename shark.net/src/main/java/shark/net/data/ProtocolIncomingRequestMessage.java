package shark.net.data;

import java.io.InputStream;

public final class ProtocolIncomingRequestMessage extends IncomingRequestMessage {
    public ProtocolIncomingRequestMessage(InputStream data, Object state) {
        super(0, Message.ProtocolMessage, data, state);
    }
}
