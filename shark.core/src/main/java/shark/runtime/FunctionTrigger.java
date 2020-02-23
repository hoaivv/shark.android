package shark.runtime;

import java.util.HashSet;

/**
 * Decribes an event which does not provide any information to its listeners and expects return from
 * the listeners
 * @param <R> type of value to be returned by listeners
 * @see FunctionEvent
 * @see ActionTrigger
 * @see ActionEvent
 */
public final class FunctionTrigger<R> {

    private HashSet<Function<R>> handlers = new HashSet<>();

    /**
     * Indicates whether the event is listened or not
     * @return true if the event is listened; otherwise false
     */
    public final boolean hasHandler() {

        synchronized (handlers) {
            return handlers.size() > 0;
        }
    }

    /**
     * Adds a listener to the event. This method have no effects if the listener is already added to
     * the event.
     * @param handler listener to be added
     */
    public final void add(Function<R> handler) {

        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers) {
            handlers.add(handler);
        }
    }

    /**
     * Removes a listener from the event. This method has no effects if the listener is not added to
     * the event.
     * @param handler listener to be removed.
     */
    public final void remove(Function<R> handler) {
        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers){
            handlers.remove(handler);
        }
    }

    private Object owner;

    /**
     * Create an event.
     * @param owner owner of the event. This object is used to protect the event from illegal access
     */
    public FunctionTrigger(Object owner) {

        this.owner = owner;
    }

    /**
     * Invokes the event listeners and does not care about their returned values.
     * @param owner owner of the event.
     * @throws IllegalAccessException throws if the provided owner is different from the owner
     * provided when the event is created.
     */
    public final void invoke(Object owner) throws IllegalAccessException {

        if (this.owner != owner) throw new IllegalAccessException("An event could only be invoked by its owner class.");

        synchronized (handlers) {
            for (Function<R> handler : handlers) handler.run();
        }
    }

    /**
     * Invokes the event listeners.
     * @param owner owner of the event.
     * @param allowedStates expected states to be returned by listeners. Be aware that if no
     *                      expected states provided the listeners will not be invoked.
     * @return true if all listener's returned values are expected; otherwise false
     * @throws IllegalAccessException throws if the provided owner is different from the owner
     * provided when the event is created.
     */
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
