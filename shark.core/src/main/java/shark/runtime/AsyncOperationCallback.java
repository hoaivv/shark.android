package shark.runtime;

@FunctionalInterface
public interface AsyncOperationCallback {
    void accept(IAsyncOperationState result, Object state);
}
