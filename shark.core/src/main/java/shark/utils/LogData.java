package shark.utils;

public class LogData {

    private Class<?> owner;
    private String trace;
    private LogType type;
    private String message;

    public Class<?> getOwner() {
        return owner;
    }

    public String getTrace() {
        return trace;
    }

    public String getMessage() {
        return message;
    }

    public LogType getType() {
        return type;
    }

    public LogData(Class<?> owner, String trace, LogType type, String message) {
        this.owner = owner;
        this.trace = trace;
        this.type = type;
        this.message = message;
    }
}
