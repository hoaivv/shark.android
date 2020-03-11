package shark.net;

@SuppressWarnings("WeakerAccess")
@FunctionalInterface
public interface ServiceRequestCallback<T> {
    void run(RequestResult result, T response, Object state);
}
