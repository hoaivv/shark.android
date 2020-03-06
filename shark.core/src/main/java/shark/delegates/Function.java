package shark.delegates;

/**
 * Describes a method which returns a value
 * @param <R> type of value to be returned
 */
@FunctionalInterface
public interface Function<R> {

    /**
     * Invokes the method
     */
    R run();
}