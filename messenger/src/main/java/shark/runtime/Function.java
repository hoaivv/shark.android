package shark.runtime;

@FunctionalInterface
public interface Function<T,R> {
    R process(T data);
}
