package shark.runtime;

public interface IAsyncOperationState {

    boolean isWaiting();
    boolean isCompleted();
    boolean isSucceed();
    boolean isFailed();

    Exception getException();
    Object getResponse();

    void registerCallback(AsyncOperationCallback callback, Object state);
    <T> void registerCallback(SimplifiedAsyncOperationCallback<T> callback, Object state, T onFailure);
}
