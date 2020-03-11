package shark.utils;

/**
 * Describes an information, which could be logged by Shark Framework Logging System
 */
public class LogData {

    private final Class<?> owner;
    private final String trace;
    private final LogType type;
    private final String message;

    /**
     * Gets owner of the log
     * @return owner of the log
     */
    public Class<?> getOwner() {
        return owner;
    }

    /**
     * Gets the trace information of the log
     * @return trace information of the log
     */
    public String getTrace() {
        return trace;
    }

    /**
     * Gets log's message
     * @return log's message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets type of the log
     * @return type of the log
     */
    public LogType getType() {
        return type;
    }

    /**
     * Create a log information
     * @param owner owner of the log
     * @param trace trace information of the log
     * @param type type of the log
     * @param message log's message
     */
    LogData(Class<?> owner, String trace, LogType type, String message) {
        this.owner = owner;
        this.trace = trace;
        this.type = type;
        this.message = message;
    }
}
