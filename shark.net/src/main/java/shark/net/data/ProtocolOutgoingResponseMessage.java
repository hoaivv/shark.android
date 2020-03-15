package shark.net.data;

import java.io.OutputStream;

public final class ProtocolOutgoingResponseMessage extends OutgoingResponseMessage {
    public ProtocolOutgoingResponseMessage(OutputStream data, Object state) {
        super(0, Message.ProtocolMessage, data, state);
    }
}
