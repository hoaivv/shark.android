package shark.runtime;

import java.util.LinkedList;

import shark.delegates.Action2;

/**
 * Provides information of an asynchronous operation managed by Shark Framework
 */
public abstract class AsyncOperationState implements IAsyncOperationState {

    class _CallbackInfo {

        final Action2<IAsyncOperationState, Object> callback;
        final Object state;

        Action2<IAsyncOperationState, Object> getCallback(){
            return callback;
        }

        Object getState() {
            return state;
        }

        _CallbackInfo(Action2<IAsyncOperationState, Object> callback, Object state)
        {
            this.callback = callback;
            this.state = state;
        }
    }

    class _CallbackConverterInfo<T> {

        final Action2<T, Object> callback;
        final T onFailure;
        final Object state;

        Object getState() {
            return state;
        }

        T getOnFailure(){
            return onFailure;
        }

        Action2<T, Object> getCallback(){
            return callback;
        }

        _CallbackConverterInfo(Action2<T, Object> callback, T onFailure, Object state){
            this.callback = callback;
            this.onFailure = onFailure;
            this.state = state;
        }
    }

    private final LinkedList<_CallbackInfo> _callbacks = new LinkedList<>();

    private boolean isWaiting = true;
    private boolean isCompleted = false;
    private boolean isSucceed = false;
    private boolean isFailed = false;
    private Exception exception = null;
    private Object response = null;

    /**
     * Indicates whether the operation is waiting or not. The operation is defined as waiting if it
     * is not started.
     * @return true if the operation is waiting; otherwise false
     */
    public final boolean isWaiting(){
        return isWaiting;
    }

    /**
     * Indicates whether the operation is completed or not.
     * @return true if the operation is completed; otherwise false
     */
    public final boolean isCompleted() {
        return isCompleted;
    }

    /**
     * Indicates whether the operation is succeed or not.
     * @return true if the operation is succeed; otherwise false
     */
    public final boolean isSucceed() {
        return isSucceed;
    }

    /**
     * Indicates whether the operation is failed or not.
     * @return true if the operation is failed; otherwise false
     */
    public final boolean isFailed() {
        return isFailed;
    }

    /**
     * Gets the exception, caused the operation to fail
     * @return An exception if the operation is failed; otherwise null
     */
    public final Exception getException() {
        return exception;
    }

    /**
     * Gets the response of the operation if it is succeed;
     * @return response of the operation if it is succeed; otherwise null
     */
    public final Object getResponse(){
        return response;
    }

    /**
     * Registers a callback to be invoked when the operation is completed
     * @param callback callback to be registered.
     * @param state object to be passed to the callback when it is invoked.
     */
    @SuppressWarnings("WeakerAccess")
    public final void registerCallback(Action2<IAsyncOperationState, Object> callback, Object state){

        if (callback == null) throw  new IllegalArgumentException("callback");

        synchronized (_callbacks) {
            if (isCompleted) {
                try { callback.run(this, state); } catch (Exception ignored) { }
            }
            else {
                _callbacks.add(new _CallbackInfo(callback, state));
            }
        }
    }

    /**
     * Registers a callback to be invoked then the operation is completed
     * @param callback callback to be registered
     * @param state object to be passed to the callback when it is invoked
     * @param onFailure value to be passed to the callback in case the operation is failed
     * @param <T> type of expected response from the operation
     */
    public final <T> void registerCallback(Action2<T, Object> callback, Object state, T onFailure) {

        if (callback == null) throw  new IllegalArgumentException("callback");

        registerCallback((result, state1) -> {

            @SuppressWarnings("unchecked") _CallbackConverterInfo<T> info = (_CallbackConverterInfo<T>) state1;

            try {
                //noinspection unchecked
                info.getCallback().run(result.isSucceed() ? (T) result.getResponse() : info.getOnFailure(), info.getState());
            }
            catch (ClassCastException e) {
                info.getCallback().run(info.getOnFailure(), info.getState());
            }
        }, new _CallbackConverterInfo<>(callback, onFailure, state));
    }

    /**
     * Notifies that the operation is succeed;
     * @param response response of the operation
     */
    protected void notifySuccess(Object response){

        synchronized (_callbacks) {
            if (isCompleted) return;

            isWaiting = false;

            this.response = response;
            isSucceed = true;

            isCompleted = true;

            while (!_callbacks.isEmpty()){
                _CallbackInfo info = _callbacks.pop();
                try { info.getCallback().run(this, info.getState()); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Notifies that the operation is failed
     * @param exception cause of operation failure
     */
    protected final void notifyFailure(Exception exception){

        synchronized (_callbacks) {
            if (isCompleted) return;

            isWaiting = false;

            this.exception = exception;
            isFailed = true;

            isCompleted = true;

            while (!_callbacks.isEmpty()) {
                _CallbackInfo info = _callbacks.pop();
                try {
                    info.getCallback().run(this, info.getState());
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Notifies that the operation is started
     */
    protected final void notifyStart(){

        synchronized (_callbacks) {
            if (isCompleted) return;
            isWaiting = false;
        }
    }
}
