package shark.runtime;

import shark.delegates.Action1;
import shark.delegates.Function1;

/**
 * Operates continuous asynchronous operation
 * @param <T> type of operation result
 */
public final class Promise<T> {

    private T result = null;
    private Action1<T> callback = null;
    private boolean resolverAllocated = false;
    private boolean resultAvailable = false;

    /**
     * Gets resolver of current promise. This method could only be invoked once so that only the
     * creator of the promise has access to its resolver
     *
     * @return resolver of current promise
     */
    public static <T> Action1<T> getResolver(Promise<T> promise) {

        synchronized (promise) {
            if (promise.resolverAllocated) return null;
            promise.resolverAllocated = true;
        }

        return new Action1<T>() {
            @Override
            public void run(T arg) {

                synchronized (promise) {

                    if (promise.resultAvailable) return;

                    promise.resultAvailable = true;
                    promise.result = arg;

                    if (promise.callback != null) {
                        promise.callback.run(promise.result);
                    }
                }
            }
        };
    }

    /**
     * Creates new instance of Promise
     */
    public Promise() {
    }

    /**
     * Creates new instance of Promise, which already completed
     *
     * @param data operation result
     */
    public Promise(T data) {
        Promise.getResolver(this).run(data);
    }

    /**
     * Gets result of the operation. This method will block the running thread until the operation
     * is completed
     *
     * @return operation result
     * @exception InterruptedException throws if the calling thread is interrupted before operation
     * is completed
     */
    public final T result() throws InterruptedException {

        while (true) {

            synchronized (this) {
                if (resultAvailable) return result;
            }

            Thread.currentThread().join(10);
        }
    }

    /**
     * Directs the operation to invoke a specified task when it is completed
     * @param task Task to be invoked when the operation is completed
     */
    public final void then(Action1<T> task) {

        synchronized (this) {
            callback = task;
            if (task != null && resultAvailable) task.run(result);
        }
    }

    /**
     * Directs the operation to invoke a specified task when it is completed
     * @param task Task to be invoked when the operation is completed
     * @param <R> Type of data to be returned by the task
     * @return an instance of {@link Promise}, which treats the provided task return data as its
     * operation result
     */
    public final <R> Promise<R> then(Function1<T, R> task){

        synchronized (this) {

            Promise<R> promise = new Promise<R>();
            Action1<R> resolver = Promise.getResolver(promise);

            if (task != null) {

                callback = data -> resolver.run(task.run(data));
                if (resultAvailable) callback.run(result);
            }
            else {
                callback = null;
            }

            return promise;
        }
    }
}
