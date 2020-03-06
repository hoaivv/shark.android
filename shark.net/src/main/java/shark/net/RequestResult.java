package shark.net;

public enum RequestResult {
    Unknown,
    OK,
    NotConnected,
    ConnectionError,
    UnrecognizedService,
    RequestIsNotTransportable,
    ResponseIsNotTransportable,
    MalformedRequestData,
    MalformedResponseData,
    ProcessingError,
    NotProceed,
    Processing,
    Proceed,
    NotAllowed,
    ProtocolError,
    Aborted
}

