package shark.net;

public class Connection {
/*
    private static Object signature = new Object();

    public static ActionEvent<ConnectionEventArgs> onInstanceConstructed = new ActionEvent<>(signature);
    public static ActionEvent<ConnectionEventArgs> onInstanceOpened = new ActionEvent<>(signature);
    public static ActionEvent<ConnectionClosedEventArgs> onInstanceClosed = new ActionEvent<>(signature);
    public static ActionEvent<ConnectionClosedEventArgs> onInstanceDestroyed = new ActionEvent<>(signature);

    public ActionEvent<ConnectionEventArgs> onOpened = new ActionEvent<>(signature);
    public ActionEvent<ConnectionClosedEventArgs> onClosed = new ActionEvent<>(signature);
    public ActionEvent<ConnectionClosedEventArgs> onDestroyed = new ActionEvent<>(signature);
    public ActionEvent<ConnectionErrorDetectedEventArgs> onErrorDetected = new ActionEvent<>(signature);
    public ActionEvent<ConnectionStateChangedEventArgs> onStateChanged = new ActionEvent<>(signature);

    private Socket _socket;
    private InputStream _inStream;
    private OutputStream _outStream;

    private LinkedList<OutgoingMessage> _outgoingMsgQueue = new LinkedList<>();
    private LinkedList<IncomingMessage> _incomingMsgQueue = new LinkedList<>();
    private LinkedList<Action1<OutgoingStream>> _outgoingActionQueue = new LinkedList<>();
    private LinkedList<Action1<IncomingStream>> _incomingActionQueue = new LinkedList<>();

    private HashMap<long, ServiceRequestState> _executingRequestStates = new HashMap<>();
    private ConnectionState _state = ConnectionState.Closed;
    private long _nextTransactionID = 1;
    private Long _isRequestingToCloseAtTimeUtc = null;
    private ConnectionCloseReason _closeReason = ConnectionCloseReason.Unknown;
    private boolean _notifyOnClosing = true;
    private Object _opLock = new Object();

    private IncomingStream _incomingStream;
    private OutgoingStream _outgoingStream;

    private LinkedList<Message> _queuedDuringHandShaking = new LinkedList<>();

    private NetworkProtocol protocol;

    public NetworkProtocol getProtocol() {
        return protocol;
    }

    private ConnectionOperations activeOperations = ConnectionOperations.None;

    public ConnectionOperations getActiveOperations() {
        return activeOperations;
    }

    private int numberOfReceivedRequests = 0;

    public int getNumberOfReceivedRequests() {
        return numberOfReceivedRequests;
    }

    private int numberOfPendingRequests = 0;

    public int getNumberOfPendingRequests() {
        return numberOfPendingRequests;
    }

    private int numberOfWaitingRequests = 0;

    public int getNumberOfWaitingRequests() {
        return numberOfWaitingRequests;
    }

    private int numberOfSentRequests = 0;

    public int getNumberOfSentRequests() {
        return numberOfSentRequests;
    }

    private int numberOfReceivedResponses = 0;

    public int getNumberOfReceivedResponses() {
        return numberOfReceivedResponses;
    }

    private int numberOfPendingResponses = 0;

    public int getNumberOfPendingResponses() {
        return numberOfPendingResponses;
    }

    private int numberOfWaitingResponses = 0;

    public int getNumberOfWaitingResponses() {
        return numberOfWaitingResponses;
    }

    private int numberOfSentResponses = 0;

    public int getNumberOfSentResponses() {
        return numberOfSentResponses;
    }

    private int numberOfProcessingMessages = 0;

    public int getNumberOfProcessingMessages() {
        return numberOfProcessingMessages;
    }

    public ConnectionState getState() {
        return _state;
    }

    private void setState(ConnectionState value) {

        if (value != _state) {
            ConnectionStateChangedEventArgs args = new ConnectionStateChangedEventArgs(_state, value);
            _state = value;
            onStateChanged.invoke(signature, args);
        }
    }

    private long lastIncomingUtc = System.currentTimeMillis();
    private long lastOutgoingUtc = System.currentTimeMillis();

    public long getLastIncomingUtc() {
        return lastIncomingUtc;
    }

    public long getLastOutgoingUtc() {
        return lastOutgoingUtc;
    }

    private ConnectionMode mode = ConnectionMode.Unknown;

    public ConnectionMode getMode() {
        return mode;
    }

    private InetSocketAddress remoteEndPoint = null;

    public InetSocketAddress getRemoteEndPoint() {
        return remoteEndPoint;
    }
    private InetSocketAddress remoteServer = null;

    public InetSocketAddress getRemoteServer() {
        return remoteServer;
    }

    public int idleTimeout = 30000;

    public long getIdleTime() {
        return System.currentTimeMillis() - getLastActiveUtc();
    }

    public long getLastActiveUtc() {
        return Math.max(lastIncomingUtc, lastOutgoingUtc);
    }

    public long getIncomingIdleTime() {
        return System.currentTimeMillis() - lastIncomingUtc;
    }

    public long getOutgoingIdleTime() {
        return System.currentTimeMillis() - lastOutgoingUtc;
    }

    public boolean isTimeout() {
        return getIncomingIdleTime() > idleTimeout;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (_state != ConnectionState.Closed) _endClose();

        ConnectionClosedEventArgs args = new ConnectionClosedEventArgs((ConnectionCloseReason.Destroying);

        try {
            onDestroyed.invoke(signature, args);
        }
        catch (Exception e) {
        }

        try {
            onInstanceDestroyed.invoke(signature, args);
        }
        catch (Exception e) {
        }

        if (Framework.debug) Log.information(Connection.class, "A connection is destroyed", "Connection: " + this);
    }

    Connection(NetworkProtocol protocol) {
        if (protocol == null) throw new IllegalArgumentException("protocol");

        if (Framework.debug) Log.information(Connection.class, "A connection is constructed", "Connection: " + this);

        Parallel.queue(() -> {
            try {
                onInstanceConstructed.invoke(signature, new ConnectionEventArgs(this));
            }
            catch (IllegalAccessException e) {
            }
        });
    }

    @Override
    public String toString() {
        NetworkProtocol handler = protocol;
        return "#" + hashCode() + (handler == null ? "" : " [" + mode + "|" + _state + "|" + (remoteEndPoint == null ? "n/a" : remoteEndPoint) + "]");
    }

    boolean handle(Socket socket) {
        if (socket == null) throw new IllegalArgumentException();

        synchronized (_opLock) {
            boolean ok = false;

            try {
                if (_state == ConnectionState.Active && socket == _socket) return true;
                if (_state != ConnectionState.Closed || socket == _socket) return false;

                ok = true;
            }
            catch (InterruptedException e) {
                ok = false;
            }
            finally {
                if (ok) ok = _endOpen(socket, null, ConnectionMode.Passive);

            }

            return ok;
        }
    }

    public <T> ServiceRequestState requestAsync(Class<T> expecting, String service, Object data, ServiceRequestCallback<T> callback, Object state, T onFailure) {

        if (expecting == null) throw new IllegalArgumentException("expecting");
        if (service == null || service.length() == 0) throw new IllegalArgumentException("service");

        long transactionId;

        synchronized (_executingRequestStates) {
            transactionId = _nextTransactionID;
            _nextTransactionID = Math.max(1,(_nextTransactionID + 1) % Long.MAX_VALUE);
        }

        ServiceRequestState requestState = new ServiceRequestState(expecting);
        if (callback != null) requestState.registerCallback(callback, state, onFailure);

        OutputStream stream;
        boolean converted = false;

        if (!(data instanceof OutputStream)) {
            try {
                stream = protocol.convert(data);
                converted = true;
            }
            catch (InterruptedException e) {
                requestState._notifyFailure(RequestResult.Aborted, e);
                return requestState;
            }
            catch (Exception e) {
                requestState._notifyFailure(RequestResult.RequestIsNotTransportable,  e);
                return requestState;
            }
        }
        else {
            stream = (OutputStream)data;
        }

        SharkOutgoingRequestMessage message = new SharkOutgoingRequestMessage(transactionId, service, stream, converted, null);

        try {
            while (_state == ConnectionState.Closing)
                Thread.currentThread().join(Workers.getTaskSleepInterval());
        }
        catch (InterruptedException e) {
            requestState._notifyFailure(RequestResult.Aborted, e);
            return requestState;
        }

        synchronized (_opLock) {
            if (_state == ConnectionState.Closed && remoteServer != null) open(remoteServer);

            if (_state != ConnectionState.Active) {
                requestState._notifyFailure(RequestResult.NotConnected, null);
                return requestState;
            }

            if (!protocol.isRequestor(this)) {
                requestState._notifyFailure(RequestResult.NotAllowed, null);
                return requestState;
            }

            try {
                return requestState;
            }
            finally {
                synchronized (_executingRequestStates) _executingRequestStates.put(transactionId, requestState);
                queue(message);
            }
        }
    }

    public <T> ServiceRequestState requestAsync(Class<T> expecting, String service, Object data, ServiceRequestCallback<T> callback, Object state) {
        return requestAsync(expecting, service, data, callback, state, null);
    }

    public <T> ServiceRequestState requestAsync(Class<T> expecting, String service, Object data, T onFailure) {
        return requestAsync(expecting, service, data, null, null, onFailure);
    }

    public <T> ServiceRequestState requestAsync(Class<T> expecting, String service, Object data) {
        return requestAsync(expecting, service, data, null, null, null);
    }

    public <T> ServiceRequestState request(Class<T> expecting, String service, Object data) {

        ServiceRequestState state = requestAsync(expecting, service, data, null, null, null);

        try {
        while (!state.isCompleted())
            Thread.currentThread().join(Workers.getTaskSleepInterval());
        }
        catch (InterruptedException e) {
        }

        return state;
    }

    public <T> T request(Class<T> expecting, String service, Object data, T onFailure) {
        ServiceRequestState state = request(expecting, service, data);

        try {
            return state.getResult() == RequestResult.OK ? (T) state.getResponse() : onFailure;
        }
        catch (Exception e) {
            return onFailure;
        }
    }

    public boolean open(InetAddress address, int port) {
        if (address == null) throw new IllegalArgumentException("address");
        return open(new InetSocketAddress(address, port));
    }

    public boolean open(SocketAddress serverEndPoint) {
        if (serverEndPoint == null) throw new IllegalArgumentException();

        synchronized (_opLock) {
            if (_state == ConnectionState.Active && remoteServer != null && remoteServer.toString() == serverEndPoint.toString()) return true;
            if (_state != ConnectionState.Closed || _state == ConnectionState.Starting) return false;

            while (activeOperations != ConnectionOperations.None) Thread.currentThread().join(Workers.getTaskSleepInterval());
            setState(ConnectionState.Starting);

            if (Framework.debug) Log.information(Connection.class, "Trying to establish a connection", "Operation: opening", "Target: " + serverEndPoint);
        }


        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(serverEndPoint, idleTimeout);

            if (!socket.isConnected()) {

                if (Framework.debug) Log.warning(Connection.class, "Connection is timed out", "Operation: opening", "Target: " + remoteServer);

                socket.close();
                setState(ConnectionState.Closed);

                return false;
            }

            return _endOpen(socket, serverEndPoint, ConnectionMode.Active);
        }
        catch (Exception e) {
            if (Framework.debug) Log.warning(Connection.class,"Could not establish a connection", "Operation: opening", "Target: " + serverEndPoint);

            if (socket != null) try { socket.close(); } catch (IOException e2) { }

            setState(ConnectionState.Closed);

            return false;
        }
    }

    public boolean reconnect() {
        synchronized (_opLock) {
            if (remoteServer == null || (_state != ConnectionState.Active && _state != ConnectionState.Handshaking)) return false;

            _inStream.close();
            _outStream.close();
            _socket.close();

        }
    }
*/
}
