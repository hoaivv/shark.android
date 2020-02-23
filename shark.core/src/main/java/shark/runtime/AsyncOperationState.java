package shark.runtime;

import java.util.LinkedList;

/**
 * Provides information of an asynchronous operation managed by Shark Framework
 */
public abstract class AsyncOperationState implements IAsyncOperationState {

    class _CallbackInfo {

        Action.Two<IAsyncOperationState, Object> callback;
        Object state;

        public Action.Two<IAsyncOperationState, Object> getCallback(){
            return callback;
        }

        public Object getState() {
            return state;
        }

        public _CallbackInfo(Action.Two<IAsyncOperationState, Object> callback, Object state)
        {
            this.callback = callback;
            this.state = state;
        }
    }

    class _CallbackConverterInfo<T> {

        Action.Two<T, Object> callback;
        T onFailure;
        Object state;

        public Object getState() {
            return state;
        }

        public T getOnFailure(){
            return onFailure;
        }

        public Action.Two<T, Object> getCallback(){
            return callback;
        }

        public _CallbackConverterInfo(Action.Two<T, Object> callback, T onFailure, Object state){
            this.callback = callback;
            this.onFailure = onFailure;
            this.state = state;
        }
    }

    LinkedList<_CallbackInfo> _callbackInfos = new LinkedList<>();

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
    public final void registerCallback(Action.Two<IAsyncOperationState, Object> callback, Object state){

        if (callback == null) throw  new IllegalArgumentException("callback");

        synchronized (_callbackInfos) {
            if (isCompleted) {
                try { callback.run(this, state); } catch (Exception e) { }
            }
            else {
                _callbackInfos.add(new _CallbackInfo(callback, state));
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
    public final <T> void registerCallback(Action.Two<T, Object> callback, Object state, T onFailure) {

        if (callback == null) throw  new IllegalArgumentException("callback");

        registerCallback(new Action.Two<IAsyncOperationState, Object>() {
            @Override
            public void run(IAsyncOperationState result, Object state) {

                _CallbackConverterInfo<T> info = (_CallbackConverterInfo<T>)state;

                try {
                    info.getCallback().run(result.isSucceed() ? (T)result.getResponse() : info.getOnFailure(), info.getState());
                }
                catch (ClassCastException e) {
                    info.getCallback().run(info.getOnFailure(), info.getState());
                }
            }
        }, new _CallbackConverterInfo<T>(callback, onFailure, state));
    }

    /**
     * Notifies that the operation is succeed;
     * @param response response of the operation
     */
    protected void notifySuccess(Object response){

        synchronized (_callbackInfos) {
            if (isCompleted) return;

            isWaiting = false;

            this.response = response;
            isSucceed = true;

            isCompleted = true;

            while (!_callbackInfos.isEmpty()){
                _CallbackInfo info = _callbackInfos.pop();
                try { info.getCallback().run(this, info.getState()); } catch (Exception e) {}
            }
        }
    }

    /**
     * Notifies that the operation is failed
     * @param exception cause of operation failure
     */
    protected final void notifyFailure(Exception exception){

        synchronized (_callbackInfos) {
            if (isCompleted) return;

            isWaiting = false;

            this.exception = exception;
            isFailed = true;

            isCompleted = true;

            while (!_callbackInfos.isEmpty()) {
                _CallbackInfo info = _callbackInfos.pop();
                try {
                    info.getCallback().run(this, info.getState());
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Notifies that the operation is started
     */
    protected final void notifyStart(){

        synchronized (_callbackInfos) {
            if (isCompleted) return;
            isWaiting = false;
        }
    }
}
