package shark.runtime.events;

import java.util.HashSet;

import shark.delegates.Action1;

/**
 * Describes an event which provides one argument to its listeners and expects no return from the listeners
 * @param <T> type of event argument to be passed to listeners
 * @see FunctionEvent
 * @see ActionTrigger
 * @see FunctionTrigger
 */
public final class ActionEvent<T> {

    private final HashSet<Action1<T>> handlers = new HashSet<>();
    private boolean invokerAllocated = false;

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
    public final void add(Action1<T> handler) {

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
    @SuppressWarnings("unused")
    public final void remove(Action1<T> handler) {
        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers) {
            handlers.remove(handler);
        }
    }

    /**
     * Create an event.
     */
    public ActionEvent() {
    }

    /**
     * Gets the invoker of the event. This method could only be called once per instance to ensure
     * only owner of the event has access to its invoker
     * @param event event, invoker of which to be returned
     * @param <T> type of event argument
     * @return invoker of the event if on first call; otherwise null
     */
    public static <T> Action1<T> getInvoker(ActionEvent<T> event) {
        synchronized (event) {
            if (event.invokerAllocated) return null;
            event.invokerAllocated = true;
        }

        return arg -> {
            synchronized (event.handlers) {
                for (Action1<T> handler : event.handlers) handler.run(arg);
            }
        };
    }
}
