package shark.delegates;

/**
 * Describes a method with no return value and accept 2 parameters
 * @param <T1> type of parameter 1
 * @param <T2> type of parameter 2
 */
@FunctionalInterface
public interface Action2<T1,T2> {

    /**
     * Invokes the method
     * @param arg1 parameter 1
     * @param arg2 parameter 2
     */
    void run(T1 arg1, T2 arg2);
}
