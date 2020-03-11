package shark.runtime.events;

import java.util.Collections;
import java.util.HashSet;

import shark.delegates.Function;
import shark.delegates.Function1;

/**
 * Describes an event which does not provide any information to its listeners and expects return from
 * the listeners
 * @param <R> type of value to be returned by listeners
 * @see FunctionEvent
 * @see ActionTrigger
 * @see ActionEvent
 */
public final class FunctionTrigger<R> {

    private final HashSet<Function<R>> handlers = new HashSet<>();
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

    /**
     * Create an event.
     */
    public FunctionTrigger() {
    }

    /**
     * Gets invoker of a trigger. This method could only be called once per trigger to ensure only
     * owner of the trigger has access to its invoker.
     * @param trigger trigger, invoker of which to be returned
     * @param <R> type of state returned by trigger handlers
     * @return invoker of the trigger on the first call; otherwise null
     */
    public static <R> Function1<R[], Boolean> getInvoker(FunctionTrigger<R> trigger) {

        synchronized (trigger) {
            if (trigger.invokerAllocated) return null;
            trigger.invokerAllocated = true;
        }

        return allowedStates -> {
            if (allowedStates == null) {
                synchronized (trigger.handlers) {
                    for (Function<R> handler : trigger.handlers) handler.run();
                }

                return true;
            }
            if (allowedStates.length == 0) return false;

            HashSet<R> states = new HashSet<>();
            Collections.addAll(states, allowedStates);

            synchronized (trigger.handlers) {
                for (Function<R> handler : trigger.handlers)
                    if (!states.contains(handler.run())) return false;
            }

            return true;
        };
    }
}
