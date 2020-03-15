package shark.net.data;

import java.io.InputStream;

import shark.net.RequestResult;

public final class SharkIncomingResponseMessage extends IncomingResponseMessage {

    private RequestResult result;

    public RequestResult getResult() {
        return result;
    }

    public SharkIncomingResponseMessage(long transactionId, RequestResult result, InputStream data) {
        super(transactionId, Message.SharkMessage, data, null);
        this.result = result;
    }
}
