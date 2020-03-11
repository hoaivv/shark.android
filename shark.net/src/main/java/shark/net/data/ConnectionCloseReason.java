package shark.net.data;

public enum ConnectionCloseReason {
    Unknown,
    Disposed,
    RequestedByRemoteMachine,
    ErrorDuringProcessing,
    Normal,
    PingingFailed,
    Timeout,
    RequestedByApplication,
    ConnectionInterrupted,
    ErrorDuringWriting,
    ErrorDuringReading,
    ProtocolViolation,
    ErrorDuringChecking,
    Destroying
}
