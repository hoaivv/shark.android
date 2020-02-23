package shark.utils;

/**
 * Describes types of log understanded by Shark Framework Logging System
 */
public enum LogType {

    /**
     * A log type which provides information of an error
     */
    Error,

    /**
     * A log type which provides information of an unwanted or a potential dangerous behavior
     */
    Warning,

    /**
     * A log type which provides harmless information
     */
    Information
}
