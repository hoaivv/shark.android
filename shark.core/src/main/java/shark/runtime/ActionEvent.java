package shark.runtime;

import java.util.HashSet;

public final class ActionEvent<T> {

    private HashSet<Action.One<T>> handlers = new HashSet<>();

    public final boolean hasHandler() {

        synchronized (handlers) {
            return handlers.size() > 0;
        }
    }

    public final void add(Action.One<T> handler) {

        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers) {
            handlers.add(handler);
        }
    }

    public final void remove(Action.One<T> handler) {
        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers) {
            handlers.remove(handler);
        }
    }

    private Object owner;

    public ActionEvent(Object owner) {

        this.owner = owner;
    }

    public final void invoke(Object owner, T eventArgs) throws IllegalAccessException {

        if (this.owner != owner) throw new IllegalAccessException("An event could only be invoked by its owner class.");

        synchronized (handlers) {
            for (Action.One<T> handler : handlers) handler.run(eventArgs);
        }
    }
}
