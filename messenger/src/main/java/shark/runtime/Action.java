package shark.runtime;

@FunctionalInterface
public interface Action<T> {
    void process(T data);
}
