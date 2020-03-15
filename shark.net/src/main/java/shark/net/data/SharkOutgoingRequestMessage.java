package shark.net.data;

import java.io.OutputStream;

public final class SharkOutgoingRequestMessage extends OutgoingRequestMessage {

    private final String serviceName;

    public String getServiceName() {
        return serviceName;
    }

    private final boolean dataConverted;

    public boolean isDataConverted() {
        return dataConverted;
    }

    public SharkOutgoingRequestMessage(long transactionId, String serviceName, OutputStream data, boolean isDataConverted, Object state) {
        super(transactionId, Message.SharkMessage, data, state);
        this.serviceName = serviceName;
        this.dataConverted = isDataConverted;
    }
}
