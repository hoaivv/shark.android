package shark.runtime;

@SuppressWarnings("WeakerAccess")
@FunctionalInterface
public interface Task {
    void run(Object state) throws InterruptedException;
}
