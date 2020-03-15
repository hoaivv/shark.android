package shark.net.data;

import java.io.OutputStream;

public final class SharkOutgoingRequestMessage extends OutgoingRequestMessage {

    private String serviceName;

    public String getServiceName() {
        return serviceName;
    }

    private boolean dataConvereted;

    public boolean isDataConvereted() {
        return dataConvereted;
    }

    public SharkOutgoingRequestMessage(long transactionId, String serviceName, OutputStream data, boolean isDataConverted, Object state) {
        super(transactionId, Message.SharkMessage, data, state);
        this.serviceName = serviceName;
        this.dataConvereted = isDataConverted;
    }
}
