package shark.runtime;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class Promise<T> {

    LinkedList<T> results = new LinkedList<>();
    Action<T> callback = null;

    ReentrantLock lock = new ReentrantLock();

    public void resolve(T data){

        try {
            lock.lock();

            if (callback == null) {
                results.add(data);
            }
            else {
                callback.process(data);
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void then(Action<T> consumer) {

        try{
            lock.lock();

            this.callback = consumer;

            if (consumer != null) while (!results.isEmpty()) consumer.process(results.pop());
        }
        finally {
            lock.unlock();
        }
    }

    public <R> Promise<R> then(final Function<T, R> consumer){

        try{
            lock.lock();

            final Promise<R> promise = new Promise<R>();

            if (consumer != null) {

                callback = consumer == null ? null : new Action<T>() {
                    @Override
                    public void process(T data) {
                        promise.resolve(consumer.process(data));
                    }
                };

                while (!results.isEmpty()) callback.process(results.pop());
            }
            else {
                callback = null;
            }

            return promise;
        }
        finally {
            lock.unlock();
        }
    }
}
