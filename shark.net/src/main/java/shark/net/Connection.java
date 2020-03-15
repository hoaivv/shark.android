package shark.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;

import shark.Framework;
import shark.components.IServiceRequestInfo;
import shark.components.InvalidServiceDataException;
import shark.components.NotProceedServiceException;
import shark.delegates.Action1;
import shark.net.data.ConnectionCloseReason;
import shark.net.data.ConnectionClosedEventArgs;
import shark.net.data.ConnectionErrorDetectedEventArgs;
import shark.net.data.ConnectionEventArgs;
import shark.net.data.ConnectionStateChangedEventArgs;
import shark.net.data.IncomingMessage;
import shark.net.data.Message;
import shark.net.data.OutgoingMessage;
import shark.net.data.ProtocolIncomingRequestMessage;
import shark.net.data.ProtocolIncomingResponseMessage;
import shark.net.data.ProtocolOutgoingRequestMessage;
import shark.net.data.ProtocolOutgoingResponseMessage;
import shark.net.data.SharkIncomingRequestMessage;
import shark.net.data.SharkIncomingResponseMessage;
import shark.net.data.SharkOutgoingRequestMessage;
import shark.net.data.SharkOutgoingResponseMessage;
import shark.runtime.Parallel;
import shark.runtime.Service;
import shark.runtime.ServiceExecutionResult;
import shark.runtime.ServiceExecutionState;
import shark.runtime.Services;
import shark.runtime.events.ActionEvent;
import shark.utils.Log;

@SuppressWarnings("SpellCheckingInspection")
public class Connection implements Closeable {

    public static class Operations {
        public static final int None = 0;
        public static final int Reading = 1;
        public static final int Writing = 2;
        public static final int Processing = 4;
        public static final int Checking = 8;
    }

    public static final ActionEvent<ConnectionEventArgs> onInstanceConstructed = new ActionEvent<>();
    private static final Action1<ConnectionEventArgs> onInstanceConstructedInvoker = ActionEvent.getInvoker(onInstanceConstructed);

    public static final ActionEvent<ConnectionEventArgs> onInstanceOpened = new ActionEvent<>();
    private static final Action1<ConnectionEventArgs> onInstanceOpenedInvoker = ActionEvent.getInvoker(onInstanceOpened);

    public static final ActionEvent<ConnectionClosedEventArgs> onInstanceClosed = new ActionEvent<>();
    private static final Action1<ConnectionClosedEventArgs> onInstanceClosedInvoker = ActionEvent.getInvoker(onInstanceClosed);

    public static final ActionEvent<ConnectionClosedEventArgs> onInstanceDestroyed = new ActionEvent<>();
    private static final Action1<ConnectionClosedEventArgs> onInstanceDestroyedInvoker = ActionEvent.getInvoker(onInstanceDestroyed);

    public final ActionEvent<ConnectionEventArgs> onOpened = new ActionEvent<>();
    private final Action1<ConnectionEventArgs> onOpenedInvoker = ActionEvent.getInvoker(onOpened);

    public final ActionEvent<ConnectionClosedEventArgs> onClosed = new ActionEvent<>();
    private final Action1<ConnectionClosedEventArgs> onClosedInvoker = ActionEvent.getInvoker(onClosed);

    public final ActionEvent<ConnectionClosedEventArgs> onDestroyed = new ActionEvent<>();
    private final Action1<ConnectionClosedEventArgs> onDestroyedInvoker = ActionEvent.getInvoker(onDestroyed);

    public final ActionEvent<ConnectionErrorDetectedEventArgs> onErrorDetected = new ActionEvent<>();
    private final Action1<ConnectionErrorDetectedEventArgs> onErrorDetectedInvoker = ActionEvent.getInvoker(onErrorDetected);

    public final ActionEvent<ConnectionStateChangedEventArgs> onStateChanged = new ActionEvent<>();
    private final Action1<ConnectionStateChangedEventArgs> onStateChangedInvoker = ActionEvent.getInvoker(onStateChanged);

    private Socket socket;

    private LinkedList<OutgoingMessage> outgoingMsgQueue = new LinkedList<>();
    private LinkedList<IncomingMessage> incomingMsgQueue = new LinkedList<>();
    private LinkedList<Action1<OutputStream>> outgoingActionQueue = new LinkedList<>();
    private LinkedList<Action1<InputStream>> incomingActionQueue = new LinkedList<>();

    private HashMap<Long, ServiceRequestState> executingRequestStates = new HashMap<>();
    private ConnectionState state = ConnectionState.Closed;
    private long nextTransactionID = 1;
    private Long isRequestingToCloseAtTimeUtc = null;
    private ConnectionCloseReason closeReason = ConnectionCloseReason.Unknown;
    private boolean notifyOnClosing = true;
    private Object opLock = new Object();

    private InputStream incomingStream;
    private OutputStream outgoingStream;

    private LinkedList<Message> queuedDuringHandShaking = new LinkedList<>();

    private NetworkProtocol protocol;

    public NetworkProtocol getProtocol() {
        return protocol;
    }

    private int activeOperations = Operations.None;

    public int getActiveOperations() {
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
        return state;
    }

    private void setState(ConnectionState value) {

        if (value != state) {
            ConnectionStateChangedEventArgs args = new ConnectionStateChangedEventArgs(this, state, value);
            state = value;
            onStateChangedInvoker.run(args);
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

    private SocketAddress remoteEndPoint = null;

    public SocketAddress getRemoteEndPoint() {
        return remoteEndPoint;
    }
    private SocketAddress remoteServer = null;

    public SocketAddress getRemoteServer() {
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

        if (state != ConnectionState.Closed) endClose();

        ConnectionClosedEventArgs args = new ConnectionClosedEventArgs(this, this.remoteServer, ConnectionCloseReason.Destroying);

        try {
            onDestroyedInvoker.run(args);
        }
        catch (Exception e) {
        }

        try {
            onInstanceDestroyedInvoker.run(args);
        }
        catch (Exception e) {
        }

        if (Framework.debug) Log.information(Connection.class, "A connection is destroyed", "Connection: " + this);
    }

    Connection(NetworkProtocol protocol) {
        if (protocol == null) throw new IllegalArgumentException("protocol");

        if (Framework.debug) Log.information(Connection.class, "A connection is constructed", "Connection: " + this);

        try { Parallel.queue(() -> onInstanceConstructedInvoker.run(new ConnectionEventArgs(this))); } catch (InterruptedException ignored) {}
    }

    @Override
    public String toString() {
        NetworkProtocol handler = protocol;
        return "#" + hashCode() + (handler == null ? "" : " [" + mode + "|" + state + "|" + (remoteEndPoint == null ? "n/a" : remoteEndPoint) + "]");
    }

    boolean _handle(Socket socket) {

        if (socket == null) throw new IllegalArgumentException();

        synchronized (opLock) {
            boolean ok = false;

            try {
                if (state == ConnectionState.Active && this.socket == socket) return true;
                if (state != ConnectionState.Closed || this.socket == socket) return false;

                ok = true;
            }
            catch (Exception e) {
                ok = false;
            }
            finally {
                if (ok) ok = endOpen(socket, null, ConnectionMode.Passive);

            }

            return ok;
        }
    }

    public <T> ServiceRequestState requestAsync(Class<T> expecting, String service, Object data, ServiceRequestCallback<T> callback, Object state, T onFailure) throws InterruptedException {

        if (expecting == null) throw new IllegalArgumentException("expecting");
        if (service == null || service.length() == 0) throw new IllegalArgumentException("service");

        long transactionId;

        synchronized (executingRequestStates) {
            transactionId = nextTransactionID;
            nextTransactionID = Math.max(1,(nextTransactionID + 1) % Long.MAX_VALUE);
        }

        ServiceRequestState requestState = new ServiceRequestState(expecting);
        if (callback != null) requestState.registerCallback(callback, state, onFailure);

        OutputStream stream;
        boolean converted = false;

        if (!(data instanceof ByteArrayOutputStream)) {
            try {
                stream = protocol.convert(data);
                converted = true;
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
            while (state == ConnectionState.Closing) Parallel.sleep();
        }
        catch (InterruptedException e) {
            requestState._notifyFailure(RequestResult.Aborted, e);
            return requestState;
        }

        synchronized (opLock) {
            if (state == ConnectionState.Closed && remoteServer != null) open(remoteServer);

            if (state != ConnectionState.Active) {
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
                synchronized (executingRequestStates) {
                    executingRequestStates.put(transactionId, requestState);
                }
                queue(message);
            }
        }
    }

    public <T> ServiceRequestState requestAsync(Class<T> expecting, String service, Object data, ServiceRequestCallback<T> callback, Object state) throws InterruptedException {
        return requestAsync(expecting, service, data, callback, state, null);
    }

    public <T> ServiceRequestState requestAsync(Class<T> expecting, String service, Object data, T onFailure) throws InterruptedException {
        return requestAsync(expecting, service, data, null, null, onFailure);
    }

    public <T> ServiceRequestState requestAsync(Class<T> expecting, String service, Object data) throws InterruptedException {
        return requestAsync(expecting, service, data, null, null, null);
    }

    public <T> ServiceRequestState request(Class<T> expecting, String service, Object data) throws InterruptedException {

        ServiceRequestState state = requestAsync(expecting, service, data, null, null, null);

        try {
            while (!state.isCompleted()) Parallel.sleep();
        }
        catch (InterruptedException e) {
        }

        return state;
    }

    public <T> T request(Class<T> expecting, String service, Object data, T onFailure) throws InterruptedException {
        ServiceRequestState state = request(expecting, service, data);

        try {
            return state.getResult() == RequestResult.OK ? (T) state.getResponse() : onFailure;
        }
        catch (Exception e) {
            return onFailure;
        }
    }

    public boolean open(SocketAddress serverEndPoint) throws InterruptedException {

        if (serverEndPoint == null) throw new IllegalArgumentException();

        synchronized (opLock) {
            if (state == ConnectionState.Active && remoteServer != null && remoteServer.toString() == serverEndPoint.toString()) return true;
            if (state != ConnectionState.Closed || state == ConnectionState.Starting) return false;

            while (activeOperations != Operations.None) Parallel.sleep();
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

            return endOpen(socket, serverEndPoint, ConnectionMode.Active);
        }
        catch (Exception ignored1) {
            if (Framework.debug) Log.warning(Connection.class,"Could not establish a connection", "Operation: opening", "Target: " + serverEndPoint);

            if (socket != null) try { socket.close(); } catch (IOException ignored2) { }

            setState(ConnectionState.Closed);

            return false;
        }
    }

    public boolean reconnect() {

        synchronized (opLock)
        {
            if (remoteServer == null || (state != ConnectionState.Active && state != ConnectionState.Handshaking))
                return false;

            if (incomingStream != null) try { incomingStream.close(); } catch (IOException ignored) {}
            if (outgoingStream != null) try { outgoingStream.close(); } catch (IOException ignored) {}
            try { socket.close(); } catch (IOException ignored) {}

            Socket client = new Socket();

            try {

                client.connect(remoteServer, idleTimeout);

                if (!client.isConnected()) {
                    if (Framework.debug && Framework.log) Log.warning(Connection.class,
                            "Connection is timed out",
                            "Operation: Reconnecting"
                    );

                    if (closeReason == ConnectionCloseReason.Unknown)
                        closeReason = ConnectionCloseReason.ConnectionInterrupted;
                    setState(ConnectionState.Closing);
                    return false;
                }

                client.setTcpNoDelay(true);
                client.setSoTimeout(idleTimeout);
            }
            catch (Exception ignored1) {

                if (client.isConnected()) try { client.close(); } catch (Exception ignored2){}

                if (closeReason == ConnectionCloseReason.Unknown)
                    closeReason = ConnectionCloseReason.ConnectionInterrupted;
                setState(ConnectionState.Closing);

                return false;
            }


            socket = client;

            try
            {
                incomingStream = protocol.getInput(socket, mode);
                outgoingStream = protocol.getOutput(socket, mode);
            }
            catch (Exception ignored1)
            {
                if (Framework.debug && Framework.log) Log.warning(Connection.class,
                        "Connection is interrupted",
                        "Operation: Reconnecting"
                );

                try { socket.close(); } catch (IOException ignored2) {}

                if (closeReason == ConnectionCloseReason.Unknown) closeReason = ConnectionCloseReason.ConnectionInterrupted;
                setState(ConnectionState.Closing);

                return false;
            }

            return true;
        }
    }

    public void wait(int count) throws IOException, InterruptedException {

        long deadline = System.currentTimeMillis() + idleTimeout;

        while (incomingStream.available() < count && deadline > System.currentTimeMillis()) Parallel.sleep(10);
        if (incomingStream.available() < count) throw new SocketTimeoutException();
    }

    public void close() {
        close(0);
    }

    public void close(long timeout) {

        if (timeout < 0) throw new IllegalArgumentException();

        synchronized (opLock)
        {
            if (state == ConnectionState.Active && protocol.isClosable(this))
            {
                if (closeReason == ConnectionCloseReason.Unknown) closeReason = ConnectionCloseReason.RequestedByApplication;

                isRequestingToCloseAtTimeUtc = System.currentTimeMillis() + timeout;
                setState(ConnectionState.Closing);
            }
        }
    }

    public boolean queue(Message message) {
        synchronized (opLock)
        {
            if (message == null) throw new IllegalArgumentException();

            if (state == ConnectionState.Handshaking)
            {
                synchronized (queuedDuringHandShaking) {
                    queuedDuringHandShaking.add(message);
                }

                return true;
            }

            if (state != ConnectionState.Active) return false;

            switch (message.getType() & (Message.Incoming | Message.Outgoing))
            {
                case Message.Incoming:

                    synchronized (incomingMsgQueue)
                    {
                        switch (message.getType() & (Message.Request | Message.Response))
                        {
                            case Message.Request:

                                numberOfReceivedRequests++;
                                numberOfWaitingRequests++;

                            break;

                            case Message.Response:

                                numberOfReceivedResponses++;
                                numberOfWaitingResponses++;

                            break;

                            default: return false;
                        }

                        incomingMsgQueue.add((IncomingMessage)message);

                        if (protocol.isProcessingOperationNeeded(this) && (activeOperations & Operations.Processing) == Operations.None)
                        {
                            activeOperations |= Operations.Processing;
                            NetworkOperator.enqueueProcessor(beginProcess);
                        }
                    }

                break;

                case Message.Outgoing:

                    synchronized (outgoingMsgQueue)
                    {

                        switch (message.getType() & (Message.Request | Message.Response))
                        {
                            case Message.Request:

                                numberOfPendingRequests++;

                            break;

                            case Message.Response:

                                numberOfPendingResponses++;

                            break;

                            default: return false;
                        }

                        outgoingMsgQueue.add((OutgoingMessage)message);

                        if (protocol.isWritingOperationNeeded(this) && (activeOperations & Operations.Writing) == Operations.None)
                        {
                            activeOperations |= Operations.Writing;

                            NetworkOperator.enqueueIO(write);
                        }
                    }

                break;
            }

            return true;
        }
    }

    public void writeWhenAccessible(Action1<OutputStream> action) {
        if (action == null) throw new IllegalArgumentException();
        synchronized (outgoingActionQueue){
            outgoingActionQueue.add(action);
        }
    }

    public void ReadWhenAccessible(Action1<InputStream> action) {
        if (action == null) throw new IllegalArgumentException();
        synchronized (incomingActionQueue) {
            incomingActionQueue.add(action);
        }
    }

    private void resetLastIncoming() {
        lastIncomingUtc = System.currentTimeMillis();
    }

    private void resetLastOutgoing() {
        lastOutgoingUtc = System.currentTimeMillis();
    }

    private boolean endOpen(Socket socket, SocketAddress server, ConnectionMode mode) {
        synchronized (opLock)
        {
            boolean pass = false;

            try
            {
                setState(ConnectionState.Initializing);

                remoteServer = server;
                remoteEndPoint = socket.getRemoteSocketAddress();
                mode = mode;
                isRequestingToCloseAtTimeUtc = null;

                try
                {
                    protocol.initialise(this);
                }
                catch (Exception e)
                {
                    if (!(e instanceof InterruptedException))
                    {
                        if (Framework.log) Log.error(Connection.class,
                                "Error detected",
                                "Operation: Initializing",
                                "Error: " + e.getMessage(),
                                Log.stringify(e.getStackTrace())
                        );

                        Parallel.queue(() -> onErrorDetectedInvoker.run( new ConnectionErrorDetectedEventArgs(this, e)));
                    }

                    return false;
                }

                try
                {
                    socket.setTcpNoDelay(true);
                    socket.setSoTimeout(idleTimeout);

                    this.socket = socket;
                    incomingStream = protocol.getInput(socket, mode);
                    outgoingStream = protocol.getOutput(socket, mode);
                }
                catch (Exception e)
                {
                    if (!(e instanceof InterruptedException))
                    {
                        if (Framework.debug && Framework.log) Log.warning(Connection.class,
                                "Connection is interrupted",
                                "Operation: Setting up"
                        );
                    }

                    return false;
                }

                boolean HSPass = false;

                try {

                    setState(ConnectionState.Handshaking);
                    HSPass = protocol.handshake(mode, this, incomingStream, outgoingStream);
                }
                catch (Exception e)
                {
                    if (!(e instanceof InterruptedException) && !(e instanceof SocketException) && !(e instanceof IOException) && !(e instanceof SocketTimeoutException))
                    {
                        if (Framework.log) Log.error(Connection.class,
                                "Error detected",
                                "Operation: Handshaking",
                                "State: " + state,
                                "Error: " + e.getMessage(),
                                Log.stringify(e.getStackTrace())
                        );

                        Parallel.queue(() -> onErrorDetectedInvoker.run(new ConnectionErrorDetectedEventArgs(this, e)));
                    }

                    return false;
                }

                if (!HSPass || state != ConnectionState.Handshaking || !socket.isConnected())
                {
                    if (Framework.debug && Framework.log) Log.warning(Connection.class,
                            "Handshake failed",
                            "Mode: " + mode
                    );
                    return false;
                }

                if (Framework.debug && Framework.log) Log.information(Connection.class,
                        "Handshake completed",
                        "Mode: " + mode
                );

                setState(ConnectionState.Active);

                pass = true;
                return true;
            }
            catch (InterruptedException ignored)
            {
                return false;
            }
            finally
            {
                if (pass)
                {
                    ConnectionEventArgs args = new ConnectionEventArgs(this);

                    while (queuedDuringHandShaking.size() > 0) queue(queuedDuringHandShaking.poll());

                    lastIncomingUtc = lastOutgoingUtc = System.currentTimeMillis();

                    activeOperations = Operations.Checking;
                    NetworkOperator.enqueueChecker(check, System.currentTimeMillis() + 1000);

                    if (protocol.isReadingOperationNeeded(this))
                    {
                        activeOperations |= Operations.Reading;
                        NetworkOperator.enqueueIO(read);
                    }

                    try { onOpenedInvoker.run(args); } catch (Exception ignored) {}
                    try { Parallel.queue(() -> onInstanceOpenedInvoker.run(args)); } catch (Exception ignored) {}
                }
                else
                {
                    setState(ConnectionState.Closed);

                    remoteEndPoint = null;
                    mode = ConnectionMode.Unknown;

                    if (incomingStream != null) try { incomingStream.close(); } catch (IOException ignored) {}
                    if (outgoingStream != null) try { outgoingStream.close(); } catch (IOException ignored) {}
                    if (socket != null) try { socket.close(); } catch (IOException ignored) {}

                    incomingStream = null;
                    outgoingStream = null;
                    socket = null;
                }
            }
        }
    }

    private void beginClose(ConnectionCloseReason reason) {
        synchronized (opLock)
        {
            if (state == ConnectionState.Active)
            {
                if (closeReason == ConnectionCloseReason.Unknown) closeReason = reason;

                isRequestingToCloseAtTimeUtc = null;
                state = ConnectionState.Closing;
            }
        }
    }

    private void endClose() {
        synchronized (opLock)
        {
            setState(ConnectionState.Closing);

            if (notifyOnClosing)
            {
                try
                {
                    ProtocolOutgoingRequestMessage message = protocol.generateCloseMessage();
                    if (message != null)
                    {
                            protocol.writeMessage(message, this, outgoingStream);
                            outgoingStream.flush();
                    }
                }
                catch (Exception e)
                {
                    try { Parallel.queue(() -> onErrorDetectedInvoker.run(new ConnectionErrorDetectedEventArgs(this, e))); } catch (InterruptedException ignored) {}
                }
            }

            if (incomingStream != null) try { incomingStream.close(); } catch (IOException ignored) {}
            if (outgoingStream != null) try { outgoingStream.close(); } catch (IOException ignored) {}
            if (socket != null) try { socket.close(); } catch (IOException ignored) {}

            incomingMsgQueue.clear();
            outgoingMsgQueue.clear();

            for (long key : executingRequestStates.keySet()) executingRequestStates.get(key)._notifyFailure(RequestResult.Aborted, null);

            executingRequestStates.clear();
            nextTransactionID = 1;

            synchronized (incomingActionQueue) { incomingActionQueue.clear(); }
            synchronized (outgoingActionQueue) { outgoingActionQueue.clear(); }

            try
            {
                protocol.release(this);
            }
            catch (Exception e)
            {
                if (!(e instanceof InterruptedException))
                {
                    if (Framework.log) Log.error(Connection.class,
                            "Error detected",
                            "Operation: Releasing",
                            e.getMessage(),
                            Log.stringify(e.getStackTrace())
                    );

                    try { Parallel.queue(() -> onErrorDetectedInvoker.run(new ConnectionErrorDetectedEventArgs(this, e))); } catch (InterruptedException ignored) {}
                }
            }

            ConnectionClosedEventArgs args = new ConnectionClosedEventArgs(this, remoteServer, closeReason);

            setState(ConnectionState.Closed);

            closeReason = ConnectionCloseReason.Unknown;
            notifyOnClosing = true;

            mode = ConnectionMode.Unknown;
            remoteEndPoint = null;
            isRequestingToCloseAtTimeUtc = null;

            numberOfPendingRequests = numberOfPendingResponses = numberOfProcessingMessages = 0;
            numberOfReceivedRequests = numberOfReceivedResponses = numberOfSentRequests = 0;
            numberOfSentResponses = numberOfWaitingRequests = numberOfWaitingResponses = 0;

            if (Framework.debug && Framework.log) Log.information(Connection.class, "Connection is closed");

            try { onClosedInvoker.run(args); } catch (Exception ignored) { }
            try { Parallel.queue(() -> onInstanceClosedInvoker.run(args)); } catch (Exception ignored) {}
        }
    }

    private void ping() {
        if (state != ConnectionState.Active || !protocol.isRequestor(this)) return;

        ProtocolOutgoingRequestMessage message = null;

        try
        {
            message = protocol.generatePingMessage();
        }
        catch (Exception e)
        {
            if (!(e instanceof InterruptedException))
            {
                try { Parallel.queue(() -> onErrorDetectedInvoker.run(new ConnectionErrorDetectedEventArgs(this, e))); } catch (InterruptedException ignored) {}
                beginClose(ConnectionCloseReason.PingingFailed);
            }
        }
        finally
        {
            if (message != null)
            {
                queue(message);
            }
        }
    }

    private void endProcess(IncomingMessage incomingMessage)
    {
        try
        {
            if (state != ConnectionState.Active) return;

            if ((incomingMessage.getType() & Message.Incoming) == Message.Unknown)
                throw new UnsupportedOperationException();

            int method = incomingMessage.getType() & (Message.SharkMessage | Message.ProtocolMessage);

            int type = incomingMessage.getType() & (Message.Request | Message.Response);

            switch (method)
            {
                case Message.ProtocolMessage:

                    synchronized (opLock)
                    {
                        switch (type)
                        {
                            case Message.Request:

                                if (protocol.isCloseMessage((ProtocolIncomingRequestMessage)incomingMessage))
                                {
                                    if (closeReason == ConnectionCloseReason.Unknown) closeReason = ConnectionCloseReason.RequestedByRemoteMachine;
                                    notifyOnClosing = false;
                                    close(0);

                                    break;
                                }

                                if (protocol.isPingMessage((ProtocolIncomingRequestMessage)incomingMessage))
                                {
                                    ProtocolOutgoingResponseMessage pong = protocol.answerPingMessage((ProtocolIncomingRequestMessage)incomingMessage);
                                    if (pong != null) queue(pong);

                                    break;
                                }

                                protocol.processProtocolRequest(this, (ProtocolIncomingRequestMessage)incomingMessage);

                            break;

                        case Message.Response:

                            protocol.processProtocolResponse(this, (ProtocolIncomingResponseMessage)incomingMessage);

                            break;

                        default: throw new UnsupportedOperationException();
                    }
                }

                protocol.finishProceedMessage(incomingMessage, this);

                break;

                case Message.SharkMessage:

                    switch (type)
                    {
                        case Message.Request:

                            if (incomingMessage.getTransactionId() == 0) throw new UnsupportedOperationException();

                            SharkIncomingRequestMessage request = (SharkIncomingRequestMessage)incomingMessage;

                            Service[] services = Services.get(Services.getOriginalName(request.getServiceName()));
                            Service service = services.length != 1 ? null : services[0];

                            if (service == null || !service.is(INetworkServiceHandler.class))
                            {
                                queue(new SharkOutgoingResponseMessage(request.getTransactionId(), RequestResult.UnrecognizedService, null, false, protocol.generateResponseState(request)));
                                protocol.finishProceedMessage(incomingMessage, this);

                                break;
                            }

                            Object requestData;

                            if (service.getDataClass() != ByteArrayInputStream.class)
                            {
                                try
                                {
                                    requestData = protocol.convert(request.getData(), service.getDataClass());
                                }
                                catch (Exception e)
                                {
                                    if (e instanceof InterruptedException) {
                                        queue(new SharkOutgoingResponseMessage(request.getTransactionId(), RequestResult.NotProceed, null, false, protocol.generateResponseState(request)));
                                        protocol.finishProceedMessage(incomingMessage, this);
                                    }
                                    else {
                                        queue(new SharkOutgoingResponseMessage(request.getTransactionId(), RequestResult.MalformedRequestData, null, false, protocol.generateResponseState(request)));
                                        protocol.finishProceedMessage(incomingMessage, this);
                                    }

                                    break;
                                }
                            }
                            else
                            {
                                requestData = request.getData();
                            }

                            Func<Connection, object, object, object, INetworkServiceRequestInfo> argsConstructor;

                            synchronized (mappedTypes)
                            {
                                if (!_MappedTypes.ContainsKey(service.getDataClass())) _MappedTypes[service.DataType] = (Func<Connection, object, object, object, INetworkServiceRequestInfo>)typeof(NetworkServiceRequestInfo<>).MakeGenericType(new Type[] { service.DataType }).GetMethod("GenerateActivator", BindingFlags.Static | BindingFlags.NonPublic).Invoke(null, new object[0]);
                                argsConstructor = _MappedTypes[service.DataType];
                            }

                            INetworkServiceRequestInfo args = argsConstructor(this, request.getState(), protocol.generateResponseState(request), requestData);

                            SharkOutgoingResponseMessage response = null;

                            Exception serviceException = null;
                            ServiceExecutionResult serviceResult = null;
                            IServiceRequestInfo serviceRequest = args;

                            try
                            {
                                serviceResult = service.process(serviceRequest);
                            }
                            catch (Exception e)
                            {
                                serviceException = e;
                            }
                            finally
                            {
                                try
                                {
                                    Object responseState = ((INetworkServiceRequestInfo)serviceRequest).ResponseState;

                                if (serviceException == null)
                                {
                                    OutputStream responseStream = null;

                                    boolean converted = false;

                                    if (service.getReturnClass() != ByteArrayOutputStream.class)
                                    {
                                        try
                                        {
                                            responseStream = protocol.convert(serviceResult.getExecutionResult());
                                            converted = true;
                                        }
                                        catch (Exception e)
                                        {
                                            response = new SharkOutgoingResponseMessage(request.getTransactionId(), RequestResult.ResponseIsNotTransportable, null, false, responseState);
                                            protocol.finishProceedMessage(incomingMessage, this);

                                            Log.warning(Connection.class,
                                                    "Service response is not transportable",
                                                    "Service: " + service
                                            );
                                        }
                                    }
                                    else
                                    {
                                        if (serviceResult.getExecutionResult() != null)
                                        {
                                            responseStream = (ByteArrayOutputStream)serviceResult.getExecutionResult();
                                        }
                                        else
                                        {
                                            responseStream = null;
                                        }
                                    }

                                    if (response == null)
                                    {
                                        response = new SharkOutgoingResponseMessage(request.getTransactionId(), RequestResult.OK, responseStream, converted, responseState);
                                    }
                                }
                                else if (serviceException instanceof InvalidServiceDataException)
                                {
                                    response = new SharkOutgoingResponseMessage(request.getTransactionId(), RequestResult.MalformedRequestData, null, false, responseState);

                                    Log.warning(Connection.class,
                                            "Malformed request data",
                                            "Service: " + service
                                    );

                                }
                                else if (serviceException instanceof NotProceedServiceException)
                                {
                                    response = new SharkOutgoingResponseMessage(request.getTransactionId(), RequestResult.NotProceed, null, false, responseState);
                                }
                                else
                                {
                                    if (serviceException instanceof InterruptedException)
                                    {
                                        Log.warning(Connection.class,
                                                "Service processing procedure is aborted",
                                                "Service: " + service
                                        );
                                    }
                                    else
                                    {
                                        Log.warning(Connection.class,
                                                "Error detected during service processing procedure",
                                                "Service: " + service,
                                                "Error:" + serviceException.getMessage(),
                                                Log.stringify(serviceException.getStackTrace())
                                        );
                                    }

                                    response = new SharkOutgoingResponseMessage(request.getTransactionId(), RequestResult.ProcessingError, null, false, responseState);
                                }
                            }
                            catch (InterruptedException e)
                            {
                                Object responseState = ((INetworkServiceRequestInfo)serviceRequest).ResponseState;
                                response = new SharkOutgoingResponseMessage(request.getTransactionId(), RequestResult.ProcessingError, null, false, responseState);
                            }
                            finally
                            {
                                queue(response);
                                protocol.finishProceedMessage(incomingMessage, this);
                            }
                        }

                        break;

                        case Message.Response:

                            if (incomingMessage.getTransactionId() == 0) throw new UnsupportedOperationException();

                            ServiceRequestState state = null;

                            synchronized (executingRequestStates)
                            {
                                state = executingRequestStates.containsKey(incomingMessage.getTransactionId()) ? executingRequestStates.get(incomingMessage.getTransactionId()) : null;
                                executingRequestStates.remove(incomingMessage.getTransactionId());
                            }

                            RequestResult result = ((SharkIncomingResponseMessage)incomingMessage).getResult();

                            if (state.getExpecting() != ByteArrayInputStream.class)
                            {
                                Object responseData = null;
                                Exception exception = null;

                                try
                                {
                                    responseData = (incomingMessage.getData() != null && incomingMessage.getData().available() > 0 ? protocol.convert(incomingMessage.getData(), state.getExpecting()) : null);
                                }
                                catch(Exception e)
                                {
                                    exception = e;
                                }
                                finally
                                {
                                    if (exception != null)
                                    {
                                        state._notifyFailure(exception instanceof InterruptedException ? RequestResult.Aborted : RequestResult.MalformedResponseData, exception);
                                    }
                                    else
                                    {
                                        state._notifySuccess(result, responseData);
                                    }
                                }
                            }
                            else
                            {
                                state._notifySuccess(result, incomingMessage.getData());
                            }

                            protocol.finishProceedMessage(incomingMessage, this);

                            break;

                        default: throw new UnsupportedOperationException();
                    }

                    break;

                default: throw new UnsupportedOperationException();
            }
        }
        catch (Exception e)
        {
            Log.error(Connection.class,
                    "Error detected during message processing procedure",
                    "Operation: " + Operations.Processing,
                    "Error: " + e.getMessage(),
                    Log.stringify(e.getStackTrace())
            );

            try { Parallel.queue(() -> onErrorDetectedInvoker.run(new ConnectionErrorDetectedEventArgs(this, e))); } catch (InterruptedException ignored) {}
            beginClose(ConnectionCloseReason.ErrorDuringProcessing);
        }
    }

    private void beginProcess()
    {
        IncomingMessage incomingMessage = null;

        synchronized (incomingMsgQueue)
        {
            incomingMessage = incomingMsgQueue.size() > 0 ? incomingMsgQueue.poll() : null;

            if (incomingMessage != null)
            {
                switch (incomingMessage.getType() & (Message.Request | Message.Response))
                {
                    case Message.Request: numberOfWaitingRequests--; break;
                    case Message.Response: numberOfWaitingResponses--; break;
                }
            }
        }

        synchronized (opLock)
        {
            if (state == ConnectionState.Active && protocol.isReadingOperationNeeded(this) && (activeOperations & Operations.Reading) == Operations.None)
            {
                activeOperations |= Operations.Reading;
                NetworkOperator.enqueueIO(read);
            }

            if (state == ConnectionState.Active && protocol.isProcessingOperationNeeded(this))
            {
                NetworkOperator.enqueueProcessor(beginProcess);
            }
            else
            {
                activeOperations &= ~Operations.Processing;
            }
        }

        if (incomingMessage != null)
        {
            try
            {
                synchronized (opLock) {
                    numberOfProcessingMessages++;
                }

                endProcess(incomingMessage);
            }
            finally {
                synchronized (opLock) {
                    numberOfProcessingMessages--;
                }
            }
        }
    }

    private void read()
    {
        boolean hasWaitingData = false;

        try
        {
            if (incomingStream.available() > 0)
            {
                if (state != ConnectionState.Active) return;

                Action1<InputStream> action;

                synchronized (incomingActionQueue) {
                    action = (incomingActionQueue.size() > 0) ? incomingActionQueue.poll() : null;
                }

                if (action != null)
                {
                    action.run(incomingStream);
                }
                else
                {
                    IncomingMessage message = null;

                    message = protocol.readMessage(this, incomingStream);

                    if (message != null)
                    {
                        queue(message);
                    }

                }

                hasWaitingData = incomingStream.available() > 0;
            }
        }
        catch (SocketException e)
        {
            if (Framework.debug && Framework.log) Log.information(Connection.class,
                    "Connection is interrupted",
                    "Operation: " + Operations.Reading
            );

            beginClose(ConnectionCloseReason.ConnectionInterrupted);
        }
        catch (IOException e)
        {
            if (Framework.debug && Framework.log) Log.information(Connection.class,
                    "Connection is interrupted",
                    "Operation: " + Operations.Reading
            );

            beginClose(ConnectionCloseReason.ConnectionInterrupted);
        }
        catch (ProtocolException e)
        {
            if (Framework.log) Log.warning(Connection.class,
                    "Protocol violation detected",
                    "Operation: " + Operations.Reading
            );

            beginClose(ConnectionCloseReason.ProtocolViolation);
        }
        catch (Exception e)
        {
            if (!(e instanceof InterruptedException) && !(e instanceof SocketTimeoutException))
            {
                if (Framework.log) Log.error(Connection.class,
                        "Error detected",
                        "Operation: " + Operations.Reading,
                        "Error: " + e.getMessage(),
                        Log.stringify(e.getStackTrace())
                );

                try { Parallel.queue(() -> onErrorDetectedInvoker.run(new ConnectionErrorDetectedEventArgs(this, e))); } catch (InterruptedException ignored) {}
            }

            beginClose(ConnectionCloseReason.ErrorDuringReading);
        }
        finally
        {
            synchronized (opLock)
            {
                if (state == ConnectionState.Active && protocol.isReadingOperationNeeded(this))
                {
                    if (hasWaitingData)
                    {
                        NetworkOperator.enqueueIO(read);
                    }
                    else
                    {
                        NetworkOperator.enqueueIO(read, System.currentTimeMillis() + 10);
                    }
                }
                else
                {
                    activeOperations &= ~Operations.Reading;
                }
            }
        }
    }

    private void check()
    {
        try
        {
            if (state == ConnectionState.Closed) return;

            if (state == ConnectionState.Closing)
            {
                if (isRequestingToCloseAtTimeUtc != null)
                {
                    try
                    {
                        while (isRequestingToCloseAtTimeUtc > System.currentTimeMillis())
                        {
                            incomingStream.available();
                            Parallel.sleep();
                        }
                    }
                    catch(Exception e)
                    {
                        if (Framework.debug && Framework.log) Log.information(Connection.class, "Connection is closed before closing timeout eslapsed");
                    }
                    finally
                    {
                        isRequestingToCloseAtTimeUtc = null;
                    }
                }

                synchronized (opLock)
                {
                    if (activeOperations == Operations.Checking && numberOfProcessingMessages == 0) endClose();
                }
            }

            if (state == ConnectionState.Active)
            {
                if (incomingIdleTime.TotalMilliseconds > idleTimeout)
                {
                    beginClose(ConnectionCloseReason.Timeout);
                }
                else
                {
                    if (incomingIdleTime.TotalMilliseconds > idleTimeout / 2)
                    {
                        ping();
                    }
                }
            }
        }
        catch (SocketException e)
        {
            if (Framework.debug && Framework.log) Log.information(Connection.class,
                    "Connection is interrupted",
                    "Procedure: " + Operations.Checking
            );

            beginClose(ConnectionCloseReason.ConnectionInterrupted);
        }
        catch (Exception e)
        {
            if (!(e instanceof InterruptedException))
            {
                if (Framework.log) Log.error(Connection.class,
                        "Error detected",
                        "Operation: " + Operations.Checking,
                        "Error: " + e.getMessage(),
                        Log.stringify(e.getStackTrace())
                );

                try { Parallel.queue(() -> onErrorDetectedInvoker.run(new ConnectionErrorDetectedEventArgs(this, e))); } catch (InterruptedException ignored) {}
            }

            beginClose(ConnectionCloseReason.ErrorDuringChecking);
        }
        finally
        {
            synchronized (opLock)
            {
                if (state != ConnectionState.Closed)
                {
                    NetworkOperator.enqueueChecker(check, System.currentTimeMillis() + 1000);
                }
                else
                {
                    activeOperations &= ~Operations.Checking;
                }
            }
        }
    }

    private void write()
    {
        try
        {
            if (state != ConnectionState.Active) return;

            Action1<OutputStream> action;

            synchronized (outgoingActionQueue) {
                action = outgoingActionQueue.size() > 0 ? outgoingActionQueue.poll() : null;
            }

            if (action != null)
            {
                action.run(outgoingStream);
                outgoingStream.flush();
            }
            else
            {
                OutgoingMessage message;
                synchronized (outgoingMsgQueue) {
                    message = outgoingMsgQueue.size() > 0 ? outgoingMsgQueue.peek() : null;
                }

                if (message != null)
                {
                    boolean written = protocol.writeMessage(message, this, outgoingStream);
                    outgoingStream.flush();

                    if (written)
                    {
                        if ((message.getType() & (Message.SharkMessage | Message.Request)) == (Message.SharkMessage | Message.Request))
                        {
                            executingRequestStates.get(message.getTransactionId())._notifyStart();
                        }

                        protocol.finishProceedMessage(message, this);

                        synchronized (outgoingMsgQueue)
                        {
                            outgoingMsgQueue.poll();

                            switch (message.getType() & (Message.Request | Message.Response))
                            {
                                case Message.Request:

                                    numberOfPendingRequests--;
                                    numberOfSentRequests++;
                                    break;

                                case Message.Response:

                                    numberOfPendingResponses--;
                                    numberOfSentResponses++;
                                    break;
                            }
                        }
                    }
                }
            }
        }
        catch (SocketException e)
        {
            if (Framework.debug && Framework.log) Log.information(Connection.class,
                    "Connection is interrupted",
                    "Operation: " + Operations.Writing
            );

            beginClose(ConnectionCloseReason.ConnectionInterrupted);
        }
        catch (IOException e)
        {
            if (Framework.debug && Framework.log) Log.information(Connection.class,
                    "Connection is interrupted",
                    "Operation: " + Operations.Writing
            );

            beginClose(ConnectionCloseReason.ConnectionInterrupted);
        }
        catch (ProtocolException e)
        {
            if (Framework.log) Log.warning(Connection.class,
                    "Protocol violation detected",
                    "Operation: " + Operations.Writing
            );

            beginClose(ConnectionCloseReason.ConnectionInterrupted);
        }
        catch (Exception e)
        {
            if (Framework.log) Log.error(Connection.class,
                    "Error detected",
                    "Operation: " + Operations.Writing,
                    "Error: " + e.getMessage(),
                    Log.stringify(e.getStackTrace())
            );

            try { Parallel.queue(() -> onErrorDetectedInvoker.run(new ConnectionErrorDetectedEventArgs(this, e))); } catch (InterruptedException ignored) {}

            beginClose(ConnectionCloseReason.ErrorDuringWriting);
        }
        finally
        {
            synchronized (opLock)
            {
                if (state == ConnectionState.Active && protocol.isReadingOperationNeeded(this) && (activeOperations & Operations.Reading) == Operations.None)
                {
                    activeOperations |= Operations.Reading;
                    NetworkOperator.enqueueIO(read);
                }

                if (state == ConnectionState.Active && protocol.isWritingOperationNeeded(this))
                {
                    NetworkOperator.enqueueIO(write);
                }
                else
                {
                    activeOperations &= ~Operations.Writing;
                }
            }
        }
    }
}
