package shark.net.data;

import shark.net.Connection;

public class ConnectionErrorDetectedEventArgs extends ConnectionEventArgs {

    private Exception exception;

    public Exception getException() {
        return exception;
    }

    public ConnectionErrorDetectedEventArgs(Connection connection, Exception exception) {

        super(connection);
        this.exception = exception;
    }
}
