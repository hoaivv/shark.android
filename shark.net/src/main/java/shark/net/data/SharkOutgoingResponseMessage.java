package shark.net.data;

import java.io.OutputStream;

import shark.net.RequestResult;

public final class SharkOutgoingResponseMessage extends OutgoingResponseMessage {

    private RequestResult result;

    public RequestResult getResult() {
        return result;
    }

    private boolean dataConvereted;

    public boolean isDataConvereted() {
        return dataConvereted;
    }

    public SharkOutgoingResponseMessage(long transactionId, RequestResult result, OutputStream data, boolean isDataConverted, Object state){
        super(transactionId, Message.SharkMessage, data, state);
        this.result = result;
        this.dataConvereted = isDataConverted;
    }
}
