package shark.net.data;

import java.net.SocketAddress;

import shark.net.Connection;

@SuppressWarnings("WeakerAccess")
public class ConnectionClosedEventArgs extends ConnectionEventArgs {

    private final ConnectionCloseReason reason;
    private SocketAddress remoteServer;

    public ConnectionCloseReason getReason() {
        return reason;
    }

    public SocketAddress getRemoteServer() {
        return remoteServer;
    }

    public ConnectionClosedEventArgs(Connection connection, SocketAddress remoteServer, ConnectionCloseReason reason) {

        super(connection);

        this.remoteServer = remoteServer;
        this.reason = reason;
    }
}
