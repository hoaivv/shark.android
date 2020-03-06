package shark.net;

public enum ConnectionState {
    Unknown,
    Starting,
    Initializing,
    Handshaking,
    Active,
    Closing,
    Closed
}
