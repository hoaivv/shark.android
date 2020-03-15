package shark.net.data;

import java.io.OutputStream;

import shark.net.RequestResult;

public final class SharkOutgoingResponseMessage extends OutgoingResponseMessage {

    private final RequestResult result;

    @SuppressWarnings("unused")
    public RequestResult getResult() {
        return result;
    }

    private final boolean dataConverted;

    @SuppressWarnings("unused")
    public boolean isDataConverted() {
        return dataConverted;
    }

    public SharkOutgoingResponseMessage(long transactionId, RequestResult result, OutputStream data, boolean isDataConverted, Object state){
        super(transactionId, Message.SharkMessage, data, state);
        this.result = result;
        this.dataConverted = isDataConverted;
    }
}
