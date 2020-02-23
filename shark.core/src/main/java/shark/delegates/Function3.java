package shark.delegates;

/**
 * Describes a method which returns a value and accept 3 parameters
 * @param <T1> type of parameter 1
 * @param <T2> type of parameter 2
 * @param <T3> type of parameter 3
 * @param <R> type of value to be returned
 */
@FunctionalInterface
public interface Function3<T1, T2, T3, R> {

    /**
     * Invokes the method
     * @param arg1 parameter 1
     * @param arg2 parameter 2
     * @param arg3 parameter 3
     */
    R run(T1 arg1, T2 arg2, T3 arg3);
}
