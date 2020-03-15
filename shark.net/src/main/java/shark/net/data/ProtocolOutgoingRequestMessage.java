package shark.net.data;

import java.io.OutputStream;

public final class ProtocolOutgoingRequestMessage extends OutgoingRequestMessage {
    public ProtocolOutgoingRequestMessage(OutputStream data, Object state) {
        super(0, Message.ProtocolMessage, data, state);
    }
}
