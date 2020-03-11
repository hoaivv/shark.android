package shark.net.data;

import shark.net.Connection;

@SuppressWarnings("WeakerAccess")
public class ConnectionEventArgs {

    private final Connection connection;


    public Connection getConnection() {
        return connection;
    }

    public ConnectionEventArgs(Connection connection) {
        this.connection = connection;
    }
}
