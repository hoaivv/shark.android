package shark.net.data;

import java.io.InputStream;

abstract class IncomingRequestMessage extends IncomingMessage {
    IncomingRequestMessage(long transactionId, int type, InputStream data, Object state) {
        super(transactionId, type | Message.Request, data, state);
    }
}
