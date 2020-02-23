package shark.runtime;

import java.util.HashSet;

public final class ActionTrigger {

    private HashSet<Action> handlers = new HashSet<>();

    public final boolean hasHandler() {

        synchronized (handlers) {
            return handlers.size() > 0;
        }
    }

    public final void add(Action handler) {

        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers) {
            handlers.add(handler);
        }
    }

    public final void remove(Action handler) {
        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers){
            handlers.remove(handler);
        }
    }

    private Object owner;

    public ActionTrigger(Object owner) {

        this.owner = owner;
    }

    public final void invoke(Object owner) throws IllegalAccessException {

        if (this.owner != owner) throw new IllegalAccessException("An event could only be invoked by its owner class.");

        synchronized (handlers) {
            for (Action handler : handlers) handler.run();
        }
    }
}
