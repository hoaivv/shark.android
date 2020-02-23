package shark.runtime;

import java.util.HashSet;

public final class FunctionEvent<T,R> {

    private HashSet<Function.One<T,R>> handlers = new HashSet<>();

    public final boolean hasHandler() {

        synchronized (handlers) {
            return handlers.size() > 0;
        }
    }

    public final void add(Function.One<T,R> handler) {

        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers) {
            handlers.add(handler);
        }
    }

    public final void remove(Function.One<T,R> handler) {
        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers) {
            handlers.remove(handler);
        }
    }

    private Object owner;

    public FunctionEvent(Object owner) {

        this.owner = owner;
    }

    public final boolean invoke(Object owner, T eventArgs, R... allowedStates) throws IllegalAccessException {

        if (this.owner != owner) throw new IllegalAccessException("An event could only be invoked by its owner");
        if (allowedStates == null || allowedStates.length == 0) return false;

        HashSet<R> states = new HashSet<>();
        for (R state : allowedStates) states.add(state);

        synchronized (handlers) {
            for (Function.One<T,R> handler : handlers) if (!states.contains(handler.run(eventArgs))) return false;
        }

        return true;
    }

    public final void invoke(Object owner, T eventArgs)  throws IllegalAccessException {

        if (this.owner != owner) throw new IllegalAccessException("An event could only be invoked by its owner class.");

        synchronized (handlers) {
            for (Function.One<T,R> handler : handlers) handler.run(eventArgs);
        }
    }
}
