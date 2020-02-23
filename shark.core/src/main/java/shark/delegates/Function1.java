package shark.delegates;

/**
 * Describes a method which returns a value and accept 1 parameter
 * @param <T> type of parameter
 * @param <R> type of value to be returned
 */
@FunctionalInterface
public interface Function1<T,R> {

    /**
     * Invokes the method
     * @param arg parameter
     */
    R run(T arg);
}
