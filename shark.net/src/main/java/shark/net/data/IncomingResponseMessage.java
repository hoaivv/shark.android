package shark.net.data;

import java.io.InputStream;

abstract class IncomingResponseMessage extends IncomingMessage {
    IncomingResponseMessage(long transactionId, int type, InputStream data, Object state) {
        super(transactionId, type | Message.Response, data, state);
    }
}
