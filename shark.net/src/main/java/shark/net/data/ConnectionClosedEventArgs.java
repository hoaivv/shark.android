package shark.net.data;

import java.net.InetSocketAddress;

public class ConnectionClosedEventArgs {

    private ConnectionCloseReason reason;
    private InetSocketAddress remoteServer;

    public ConnectionCloseReason getReason() {
        return reason;
    }

    public InetSocketAddress getRemoteServer() {
        return remoteServer;
    }

    ConnectionClosedEventArgs(ConnectionCloseReason reason) {
        this.reason = reason;
    }
}
