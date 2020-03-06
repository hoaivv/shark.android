package shark.net;

@FunctionalInterface
public interface ServiceRequestCallback<T> {
    public void run(RequestResult result, T response, Object state);
}
