package shark.net.data;

public abstract class Message {

    public static final int Unknown = 0;
    public static final int SharkMessage = 1;
    public static final int ProtocolMessage = 2;
    public static final int Incoming = 4;
    public static final int Outgoing = 8;
    public static final int Request = 16;
    public static final int Response = 32;

    private long transactionId;

    public long getTransactionId() {
        return transactionId;
    }

    private int type;

    public int getType() {
        return type;
    }

    private Object state;

    public Object getState() {
        return state;
    }

    Message(long transactionId, int type, Object state) {
        this.transactionId = transactionId;
        this.type = type;
        this.state = state;
    }
}
