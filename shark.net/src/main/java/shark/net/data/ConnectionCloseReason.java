package shark.net.data;

public enum ConnectionCloseReason {
    Unknown,
    Disposed,
    RequestedByRemoteMachine,
    ErrorDurringProcessing,
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
