package shark.runtime;

import java.util.Objects;

@FunctionalInterface
public interface Action<T> {
    void process(T data);
}
