package shark.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import shark.net.data.IncomingMessage;
import shark.net.data.Message;
import shark.net.data.OutgoingMessage;
import shark.net.data.ProtocolIncomingRequestMessage;
import shark.net.data.ProtocolIncomingResponseMessage;
import shark.net.data.ProtocolOutgoingRequestMessage;
import shark.net.data.ProtocolOutgoingResponseMessage;
import shark.net.data.SharkIncomingRequestMessage;
import shark.runtime.serialization.SerializationException;
import shark.runtime.serialization.Serializer;

public abstract class NetworkProtocol {

    @SuppressWarnings("WeakerAccess")
    public Serializer getSerializer() {
        return Serializer.getDefault();
    }

    @SuppressWarnings("SameReturnValue")
    public boolean isClosable(Connection connection) {
        return true;
    }

    @SuppressWarnings("WeakerAccess")
    protected InputStream getInput(Socket socket, ConnectionMode mode) throws IOException {
        return socket.getInputStream();
    }

    @SuppressWarnings("WeakerAccess")
    protected OutputStream getOutput(Socket socket, ConnectionMode mode) throws IOException {
        return socket.getOutputStream();
    }

    InputStream _getInput(Socket socket, ConnectionMode mode) throws IOException {
        return getInput(socket, mode);
    }

    OutputStream _getOutput(Socket socket, ConnectionMode mode) throws IOException {
        return getOutput(socket, mode);
    }

    @SuppressWarnings("CanBeFinal")
    private InetSocketAddress localServer = null;

    public final InetSocketAddress getLocalServer() {
        return localServer;
    }

    public abstract boolean isRequestor(Connection connection);

    public abstract boolean isResponder(Connection connection);

    public abstract IncomingMessage readMessage(Connection connection, InputStream input);

    public abstract boolean writeMessage(OutgoingMessage message, Connection connection, OutputStream output);

    public abstract boolean handshake(ConnectionMode mode, Connection connection, InputStream input, OutputStream output);

    public boolean isReadingOperationNeeded(Connection connection) {
        return (isResponder(connection) && connection.getNumberOfWaitingRequests() == 0) || (isRequestor(connection) && connection.getNumberOfSentRequests() > connection.getNumberOfReceivedResponses());
    }

    public boolean isWritingOperationNeeded(Connection connection) {
        return (isRequestor(connection) && connection.getNumberOfPendingRequests() > 0 ) || (isResponder(connection) && connection.getNumberOfPendingResponses() > 0);
    }

    public boolean isProcessingOperationNeeded(Connection connection) {
        return (isRequestor(connection) && connection.getNumberOfWaitingResponses() > 0) || (isResponder(connection) && connection.getNumberOfWaitingRequests() > 0);
    }

    @SuppressWarnings("EmptyMethod")
    public void processProtocolRequest(Connection connection, ProtocolIncomingRequestMessage message) {
    }

    @SuppressWarnings("EmptyMethod")
    public void processProtocolResponse(Connection connection, ProtocolIncomingResponseMessage message) {
    }

    @SuppressWarnings("SameReturnValue")
    public ProtocolOutgoingRequestMessage generatePingMessage() {
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    public boolean isPingMessage(ProtocolIncomingRequestMessage message) {
        return false;
    }

    @SuppressWarnings("SameReturnValue")
    public ProtocolOutgoingResponseMessage answerPingMessage(ProtocolIncomingRequestMessage message) {
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    public ProtocolOutgoingRequestMessage generateCloseMessage() {
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    public boolean isCloseMessage(ProtocolIncomingRequestMessage message)
    {
        return false;
    }

    public Object convert(InputStream stream, Class<?> type) throws SerializationException {

        Serializer serializer = getSerializer();
        if (serializer == null) throw new SerializationException("Serializer is not specified", null);

        return stream == null ? null : serializer.deserialize(stream, type);
    }

    public ByteArrayOutputStream convert(Object obj) throws SerializationException {

        Serializer serializer = getSerializer();
        if (serializer == null) throw new SerializationException("Serializer is not specified", null);

        if (obj == null) return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            serializer.serialize(stream, obj);
            return stream;
        }
        catch (SerializationException e) {

            try {stream.close(); } catch (IOException ignored) {}
            throw e;
        }
    }

    @SuppressWarnings("EmptyMethod")
    public void initialise(Connection connection) {
    }

    @SuppressWarnings("EmptyMethod")
    public void release(Connection connection) {
    }

    @SuppressWarnings("EmptyMethod")
    public void finishProceedMessage(Message message, Connection connection)
    {
    }

    @SuppressWarnings("SameReturnValue")
    public Object generateResponseState(SharkIncomingRequestMessage message) {
        return null;
    }
}
