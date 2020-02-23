package shark.runtime;

@FunctionalInterface
public interface SimplifiedAsyncOperationCallback<T> {
    void accept(T response, Object state);
}
