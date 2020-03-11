package shark.delegates;

/**
 * Describes a method with no return value and accept 3 parameters
 * @param <T1> type of parameter 1
 * @param <T2> type of parameter 2
 * @param <T3> type of parameter 3
 */
@SuppressWarnings("unused")
@FunctionalInterface
public interface Action3<T1,T2,T3> {

    /**
     * Invokes the method
     * @param arg1 parameter 1
     * @param arg2 parameter 2
     * @param arg3 parameter 3
     */
    void run(T1 arg1, T2 arg2, T3 arg3);
}
