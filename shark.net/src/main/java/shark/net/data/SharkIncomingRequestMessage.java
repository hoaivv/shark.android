package shark.net.data;

import java.io.InputStream;

public final class SharkIncomingRequestMessage extends IncomingRequestMessage {

    private final String serviceName;

    public String getServiceName() {
        return serviceName;
    }

    public SharkIncomingRequestMessage(long transactionId, String serviceName, InputStream data, Object state) {
        super(transactionId, Message.SharkMessage, data, state);
        this.serviceName = serviceName;
    }
}
