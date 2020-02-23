package shark.runtime;

import java.util.LinkedList;

public abstract class AsyncOperationState implements IAsyncOperationState {

    class _CallbackInfo {

        AsyncOperationCallback callback;
        Object state;

        public AsyncOperationCallback getCallback(){
            return callback;
        }

        public Object getState() {
            return state;
        }

        public _CallbackInfo(AsyncOperationCallback callback, Object state)
        {
            this.callback = callback;
            this.state = state;
        }
    }

    class _CallbackConverterInfo<T> {

        SimplifiedAsyncOperationCallback<T> callback;
        T onFailure;
        Object state;

        public Object getState() {
            return state;
        }

        public T getOnFailure(){
            return onFailure;
        }

        public SimplifiedAsyncOperationCallback<T> getCallback(){
            return callback;
        }

        public _CallbackConverterInfo(SimplifiedAsyncOperationCallback<T> callback, T onFailure, Object state){
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

    public final boolean isWaiting(){
        return isWaiting;
    }

    public final boolean isCompleted() {
        return isCompleted;
    }

    public final boolean isSucceed() {
        return isSucceed;
    }

    public final boolean isFailed() {
        return isFailed;
    }

    public final Exception getException() {
        return exception;
    }

    public final Object getResponse(){
        return response;
    }

    public final void registerCallback(AsyncOperationCallback callback, Object state){

        if (callback == null) throw  new IllegalArgumentException("callback");

        synchronized (_callbackInfos) {
            if (isCompleted) {
                try { callback.accept(this, state); } catch (Exception e) { }
            }
            else {
                _callbackInfos.add(new _CallbackInfo(callback, state));
            }
        }
    }

    public final <T> void registerCallback(SimplifiedAsyncOperationCallback<T> callback, Object state, T onFailure) {

        if (callback == null) throw  new IllegalArgumentException("callback");

        registerCallback(new AsyncOperationCallback() {
            @Override
            public void accept(IAsyncOperationState result, Object state) {

                _CallbackConverterInfo<T> info = (_CallbackConverterInfo<T>)state;

                try {
                    info.getCallback().accept(result.isSucceed() ? (T)result.getResponse() : info.getOnFailure(), info.getState());
                }
                catch (ClassCastException e) {
                    info.getCallback().accept(info.getOnFailure(), info.getState());
                }
            }
        }, new _CallbackConverterInfo<T>(callback, onFailure, state));
    }

    protected void notifySuccess(Object response){

        synchronized (_callbackInfos) {
            if (isCompleted) return;

            isWaiting = false;

            this.response = response;
            isSucceed = true;

            isCompleted = true;

            while (!_callbackInfos.isEmpty()){
                _CallbackInfo info = _callbackInfos.pop();
                try { info.getCallback().accept(this, info.getState()); } catch (Exception e) {}
            }
        }
    }

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
                    info.getCallback().accept(this, info.getState());
                } catch (Exception e) {
                }
            }
        }
    }

    protected final void notifyStart(){

        synchronized (_callbackInfos) {
            if (isCompleted) return;
            isWaiting = false;
        }
    }
}
