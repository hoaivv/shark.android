package shark.runtime;

import shark.delegates.Action2;

/**
 * Defines an object which provides information of an asynchronous operation managed by
 * Shark Framework
 */
public interface IAsyncOperationState {

    /**
     * Indicates whether the operation is waiting or not. The operation is defined as waiting if it
     * is not started
     * @return true if the operation is waiting; otherwise false
     */
    boolean isWaiting();

    /**
     * Indicates whether the operation is completed or not
     * @return true if the operation is completed; otherwise false
     */
    boolean isCompleted();

    /**
     * Indicates whether the operation is succeed or not
     * @return true if the operation is succeed; otherwise false
     */
    boolean isSucceed();

    /**
     * Indicates whether the operation is failed or not
     * @return true if the operation is failed; otherwise false
     */
    boolean isFailed();

    /**
     * Gets the exception, caused the operation to fail.
     * @return an exception, which caused the operation to fail if the operation is failed;
     * otherwise null
     */
    Exception getException();

    /**
     * Gets the response of the operation
     * @return response of the operation if it is succeed; otherwise null
     */
    Object getResponse();

    /**
     * Registers a callback to be invoked when the operation is completed
     * @param callback callback to be registered.
     * @param state object to be passed to the callback when it is invoked.
     */
    void registerCallback(Action2<IAsyncOperationState, Object> callback, Object state);

    /**
     * Registers a callback to be invoked then the operation is completed
     * @param callback callback to be registered
     * @param state object to be passed to the callback when it is invoked
     * @param onFailure value to be passed to the callback in case the operation is failed
     * @param <T> type of expected response from the operation
     */
    <T> void registerCallback(Action2<T, Object> callback, Object state, T onFailure);
}
