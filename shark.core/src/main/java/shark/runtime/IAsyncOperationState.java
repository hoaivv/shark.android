package shark.runtime;

public interface IAsyncOperationState {

    boolean isWaiting();
    boolean isCompleted();
    boolean isSucceed();
    boolean isFailed();

    Exception getException();
    Object getResponse();

    void registerCallback(Action.Two<IAsyncOperationState, Object> callback, Object state);
    <T> void registerCallback(Action.Two<T, Object> callback, Object state, T onFailure);
}
