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

    public Serializer getSerializer() {
        return Serializer.getDefault();
    }

    public boolean isClosable(Connection connection) {
        return true;
    }

    protected InputStream getInput(Socket socket, ConnectionMode mode) throws IOException {
        return socket.getInputStream();
    }

    protected OutputStream getOutput(Socket socket, ConnectionMode mode) throws IOException {
        return socket.getOutputStream();
    }

    InputStream _getInput(Socket socket, ConnectionMode mode) throws IOException {
        return getInput(socket, mode);
    }

    OutputStream _getOutput(Socket socket, ConnectionMode mode) throws IOException {
        return getOutput(socket, mode);
    }

    InetSocketAddress _localServer = null;

    public final InetSocketAddress getLocalServer() {
        return _localServer;
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

    public void processProtocolRequest(Connection connection, ProtocolIncomingRequestMessage message) {
    }

    public void processProtocolResponse(Connection connection, ProtocolIncomingResponseMessage message) {
    }

    public ProtocolOutgoingRequestMessage generatePingMessage() {
        return null;
    }

    public boolean isPingMessage(ProtocolIncomingRequestMessage message) {
        return false;
    }

    public ProtocolOutgoingResponseMessage answerPingMessage(ProtocolIncomingRequestMessage message) {
        return null;
    }

    public ProtocolOutgoingRequestMessage generateCloseMessage() {
        return null;
    }

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

    public void initialise(Connection connection) {
    }

    public void release(Connection connection) {
    }

    public void finishProceedMessage(Message message, Connection connection)
    {
    }

    public Object generateResponseState(SharkIncomingRequestMessage message) {
        return null;
    }
}
