package shark.delegates;

/**
 * Describes a method with no return value and accept no parameters
 */
@FunctionalInterface
public interface Action {

    /**
     * Invokes the method
     */
    void run();
}
