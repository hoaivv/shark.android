package shark.net;

import shark.runtime.AsyncOperationState;
import shark.runtime.IAsyncOperationState;

@SuppressWarnings("WeakerAccess")
public class ServiceRequestState extends AsyncOperationState {

    private RequestResult result = RequestResult.Unknown;
    private final Class<?> expecting;

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
        private final ServiceRequestCallback<T> callback;
        private final T onFailure;
        private final Object state;

        private  _CallbackConverterInfo(ServiceRequestCallback<T> callback, Object state, T onFailure) {
            this.callback = callback;
            this.state = state;
            this.onFailure = onFailure;
        }
    }

    private static <T> void _callbackConverter(IAsyncOperationState result, Object state) {

        @SuppressWarnings("unchecked") _CallbackConverterInfo<T> info = (_CallbackConverterInfo<T>)state;
        //noinspection unchecked
        info.callback.run(((ServiceRequestState)result).result, result.isSucceed() ? (T)result.getResponse() : info.onFailure, info.state);
    }

    public <T> void registerCallback(ServiceRequestCallback<T> callback, Object state, T onFailure) {
        if (callback == null) throw new IllegalArgumentException("callback");

        registerCallback(ServiceRequestState::_callbackConverter, new _CallbackConverterInfo<>(callback, state, onFailure));
    }
}
