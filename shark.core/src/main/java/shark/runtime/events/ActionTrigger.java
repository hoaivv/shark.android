package shark.runtime.events;

import java.util.HashSet;

import shark.delegates.Action;

/**
 * Describes an event which provides no information to its listeners and expects no return
 * from the listeners.
 * @see ActionEvent
 * @see FunctionTrigger
 * @see FunctionEvent
 */
@SuppressWarnings("ALL")
public final class ActionTrigger {

    private final HashSet<Action> handlers = new HashSet<>();
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

    /**
     * Create an event.
     */
    public ActionTrigger() {
    }

    /**
     * Gets invoker of a trigger. This method could only be called once to ensure only the owner of
     * the trigger has access to its invoker.
     * @param trigger trigger, invoker of which to be returned
     * @return invoker of the trigger on first call; otherwise null
     */
    public static Action getInvoker(ActionTrigger trigger) {

        synchronized (trigger) {
            if (trigger.invokerAllocated) return null;
            trigger.invokerAllocated = true;
        }

        return () -> {
            synchronized (trigger.handlers) {
                for (Action handler : trigger.handlers) handler.run();
            }
        };
    }
}
