package shark.net.data;

import java.io.OutputStream;

abstract class OutgoingRequestMessage extends OutgoingMessage {

    OutgoingRequestMessage(long transactionId, int type, OutputStream data, Object state) {
        super(transactionId, type | Message.Request, data, state);
    }
}
