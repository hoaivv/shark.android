package shark.runtime;

import java.util.HashSet;

/**
 * Describes an event which provides no information to its listeners and expects no return
 * from the listeners.
 * @see ActionEvent
 * @see FunctionTrigger
 * @see FunctionEvent
 */
public final class ActionTrigger {

    private HashSet<Action> handlers = new HashSet<>();

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
    public final void add(Action handler) {

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
    public final void remove(Action handler) {
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
    public ActionTrigger(Object owner) {

        this.owner = owner;
    }

    /**
     * Invokes the event listeners.
     * @param owner owner of the event.
     * @throws IllegalAccessException throws if the provided owner is different from the owner
     * provided when the event is created.
     */
    public final void invoke(Object owner) throws IllegalAccessException {

        if (this.owner != owner) throw new IllegalAccessException("An event could only be invoked by its owner class.");

        synchronized (handlers) {
            for (Action handler : handlers) handler.run();
        }
    }
}
