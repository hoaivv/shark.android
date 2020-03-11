package shark.runtime.events;

import java.util.Collections;
import java.util.HashSet;

import shark.delegates.Function1;
import shark.delegates.Function2;

/**
 * Describes an event which provides one argument to its listeners and expects return from the
 * listeners
 * @param <T> type of event argument to be passed to listeners
 * @param <R> type of value to be returned by listeners
 * @see ActionEvent
 * @see ActionTrigger
 * @see FunctionTrigger
 */
@SuppressWarnings("WeakerAccess")
public final class FunctionEvent<T,R> {

    private final HashSet<Function1<T,R>> handlers = new HashSet<>();
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
    public final void add(Function1<T, R> handler) {

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
    public final void remove(Function1<T,R> handler) {
        if (handler == null) throw new IllegalArgumentException("listener");

        synchronized (handlers) {
            handlers.remove(handler);
        }
    }

    /**
     * Create an event.
     */
    public FunctionEvent() {
    }

    /**
     * Gets invoker of an event. This method could only be called once per event to ensure only the
     * owner of the event has access to its invoker
     * @param event event, invoker of which to be returned
     * @param <T> type of event argument
     * @param <R> type of return state of event handler
     * @return invoker of the event on the first call; otherwise null
     */
    public static <T,R> Function2<T, R[], Boolean> getInvoker(FunctionEvent<T,R> event) {

        synchronized (event) {
            if (event.invokerAllocated) return null;
            event.invokerAllocated = true;
        }

        return (eventArgs, allowedStates) -> {

            if (allowedStates == null) {

                synchronized (event.handlers) {
                    for (Function1<T,R> handler : event.handlers) handler.run(eventArgs);
                }

                return true;
            }

            if (allowedStates.length == 0) return false;

            HashSet<R> states = new HashSet<>();
            Collections.addAll(states, allowedStates);

            synchronized (event.handlers) {
                for (Function1<T,R> handler : event.handlers) if (!states.contains(handler.run(eventArgs))) return false;
            }

            return true;
        };
    }
}
