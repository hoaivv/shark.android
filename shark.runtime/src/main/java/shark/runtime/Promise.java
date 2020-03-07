package shark.runtime;

import java.util.LinkedList;

import shark.delegates.Action1;
import shark.delegates.Function1;

/**
 * Describes a continuous asynchronous operation
 * @param <T> type of operation result
 */
public final class Promise<T> {

    LinkedList<T> results = new LinkedList<>();
    Action1<T> callback = null;


    /**
     * Notifies that the operation is completed
     * @param data operation result
     */
    public final void resolve(final T data){

        synchronized (this) {
            if (callback == null) {
                results.add(data);
            }
            else {
                callback.run(data);
            }
        }
    }

    /**
     * Creates new instance of Promise
     */
    public Promise(){
    }

    /**
     * Creates new instance of Promise, which already completed
     * @param data operation result
     */
    public Promise(T data) {
        resolve(data);
    }

    /**
     * Gets result of the operation. This method will block the running thread until the operation
     * is completed
     * @return operation result
     */
    public final T result() {

        try {

            while (true) {

                synchronized (this) {
                    if (!results.isEmpty()) return results.pop();
                }

                Thread.sleep(10);
            }
        }
        catch (InterruptedException e){
            return null;
        }
    }

    /**
     * Directs the operation to invoke a specified task when it is completed
     * @param task Task to be invoked when the operation is completed
     */
    public final void then(Action1<T> task) {

        synchronized (this) {
            callback = task;
            if (task != null) while (!results.isEmpty()) task.run(results.pop());
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

            final Promise<R> promise = new Promise<R>();

            if (task != null) {

                callback = data -> promise.resolve(task.run(data));

                while (!results.isEmpty()) callback.run(results.pop());
            }
            else {
                callback = null;
            }

            return promise;
        }
    }
}
