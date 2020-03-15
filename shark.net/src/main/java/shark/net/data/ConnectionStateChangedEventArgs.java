package shark.net.data;

import shark.net.Connection;
import shark.net.ConnectionState;

public class ConnectionStateChangedEventArgs extends ConnectionEventArgs {

    private ConnectionState previousState;

    public ConnectionState getPreviousState() {
        return previousState;
    }

    private ConnectionState currentState;

    public ConnectionState getCurrentState() {
        return currentState;
    }

    public ConnectionStateChangedEventArgs(Connection connection, ConnectionState previousState, ConnectionState currentState){
        super(connection);
        this.previousState = previousState;
        this.currentState = currentState;
    }
}
