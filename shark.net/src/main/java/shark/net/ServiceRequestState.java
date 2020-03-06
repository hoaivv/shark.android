package shark.net;

import shark.runtime.AsyncOperationState;
import shark.runtime.IAsyncOperationState;

public class ServiceRequestState extends AsyncOperationState {

    private RequestResult result = RequestResult.Unknown;
    private Class<?> expecting;

    public RequestResult getResult() {
        return result;
    }

    public Class<?> getExpecting() {
        return expecting;
    }

    void _notifyStart() {
        notifyStart();
    }

    void _notifySuccess(RequestResult result, Object response) {
        synchronized (this) {
            if (isCompleted()) return;
            this.result = result;
            notifySuccess(response);
        }
    }

    void _notifyFailure(RequestResult result, Exception e) {
        synchronized (this) {
            if (isCompleted()) return;
            this.result = result;
            notifyFailure(e);
        }
    }

    ServiceRequestState(Class<?> expecting) {
        this.expecting = expecting;
    }

    class _CallbackConverterInfo<T> {
        public ServiceRequestCallback<T> callback;
        public T onFailure;
        public Object state;

        public _CallbackConverterInfo(ServiceRequestCallback<T> callback, Object state, T onFailure) {
            this.callback = callback;
            this.state = state;
            this.onFailure = onFailure;
        }
    }

    private static <T> void _callbackConverter(IAsyncOperationState result, Object state) {

        _CallbackConverterInfo<T> info = (_CallbackConverterInfo<T>)state;
        info.callback.run(((ServiceRequestState)result).result, result.isSucceed() ? (T)result.getResponse() : info.onFailure, info.state);
    }

    public <T> void registerCallback(ServiceRequestCallback<T> callback, Object state, T onFailure) {
        if (callback == null) throw new IllegalArgumentException("callback");

        registerCallback((r, s) -> _callbackConverter(r, s), new _CallbackConverterInfo<T>(callback, state, onFailure));
    }
}
