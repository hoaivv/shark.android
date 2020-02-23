package shark.runtime;

import java.util.HashSet;

public final class FunctionTrigger<R> {

    private HashSet<Function<R>> handlers = new HashSet<>();

    public final boolean hasHandler() {

        synchronized (handlers) {
            return handlers.size() > 0;
        }
    }

    public final void add(Function<R> handler) {

        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers) {
            handlers.add(handler);
        }
    }

    public final void remove(Function<R> handler) {
        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers){
            handlers.remove(handler);
        }
    }

    private Object owner;

    public FunctionTrigger(Object owner) {

        this.owner = owner;
    }

    public final void invoke(Object owner) throws IllegalAccessException {

        if (this.owner != owner) throw new IllegalAccessException("An event could only be invoked by its owner class.");

        synchronized (handlers) {
            for (Function<R> handler : handlers) handler.run();
        }
    }

    public final boolean invoke(Object owner, R... allowedStates) throws IllegalAccessException {

        if (this.owner != owner) throw new IllegalAccessException("An event could only be invoked by its owner class.");
        if (allowedStates == null || allowedStates.length == 0) return false;

        HashSet<R> states = new HashSet<>();
        for (R state : allowedStates) states.add(state);

        synchronized (handlers) {
            for (Function<R> handler : handlers) if (!states.contains(handler.run())) return false;
        }

        return true;
    }
}
