package shark.net.data;

import shark.net.Connection;

public class ConnectionEventArgs {

    private Connection connection;


    public Connection getConnection() {
        return connection;
    }

    public ConnectionEventArgs(Connection connection) {
        this.connection = connection;
    }
}
