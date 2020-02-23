package shark.delegates;

/**
 * Describes a method with no return value and accept 1 parameter
 * @param <T> type of parameter
 */
@FunctionalInterface
public interface Action1<T> {

    /**
     * Invokes the method
     * @param arg parameter
     */
    void run(T arg);
}
