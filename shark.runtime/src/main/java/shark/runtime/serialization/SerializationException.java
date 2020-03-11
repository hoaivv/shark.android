package shark.runtime.serialization;

/**
 * Exception, throws by {@link Serializer}
 */
@SuppressWarnings("WeakerAccess")
public class SerializationException extends Exception {

    /**
     * Create an instance of {@link SerializationException}
     * @param message message to describe the exception
     * @param cause underlying exception that caused this exception to be thrown
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
