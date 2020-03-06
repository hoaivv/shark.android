package shark.runtime;

import java.util.LinkedList;

import shark.delegates.Action1;
import shark.delegates.Function1;

public final class Promise<T> {

    LinkedList<T> results = new LinkedList<>();
    Action1<T> callback = null;


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

    public Promise(){
    }

    public Promise(T data) {
        resolve(data);
    }

    public final T getResult() {

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

    public final void then(final Action1<T> consumer) {

        synchronized (this) {
            callback = consumer;
            if (consumer != null) while (!results.isEmpty()) consumer.run(results.pop());
        }
    }

    public final <R> Promise<R> then(final Function1<T, R> consumer){

        synchronized (this) {

            final Promise<R> promise = new Promise<R>();

            if (consumer != null) {

                callback = data -> promise.resolve(consumer.run(data));

                while (!results.isEmpty()) callback.run(results.pop());
            }
            else {
                callback = null;
            }

            return promise;
        }
    }
}
