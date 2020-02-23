package shark.runtime;

@FunctionalInterface
public interface Task {
    void run(Object state);
}
