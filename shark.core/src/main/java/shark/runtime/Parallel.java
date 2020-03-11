package shark.runtime;

import android.os.Looper;

import shark.delegates.Action;
import shark.delegates.Action1;

public final class Parallel {

    private class LoopCounter {

        private int count = 0;
        private boolean used = false;

        void increase() {

            synchronized (this) {
                count++;
                used = true;
            }
        }

        void decrease() {

            synchronized (this) {
                count--;
            }
        }

        boolean isWaiting() {

            synchronized (this) {
                return !used || count != 0;
            }
        }
    }

    private class Worker extends shark.runtime.Worker {

        private TaskState start(Task task, Object state, boolean repeat) {
            if (task == null) throw new IllegalArgumentException("task");

            TaskState result;

            result = registerTask(task, state, repeat);
            if (result.isWaiting()) start();

            return result;
        }
    }

    private static final Parallel singleton = new Parallel();

    private static final Worker _worker = singleton.new Worker();
    private static final Operator _operator = new Operator(100, 10, 50);

    /**
     * Gets the maximum number of queue processing threads
     * @return number of threads
     */
    public static int getMaxNumberOfQueueProcessingThreads() {
        return _operator.getMaxNumberOfThreads();
    }

    /**
     * Sets the maximum number of queue processing threads
     * @param value number of threads
     */
    public static void setMaxNumberOfQueueProcessingThreads(int value) {

        if (StoredStates.set(Parallel.class, "max-number-of-queue-processing-threads", value)) {
            _operator.setMaxNumberOfThreads(value);
        }
    }

    /**
     * Gets the threshold of queue processing thread creation
     * @return threshold
     */
    public static int getQueueProcessingThreadCreationThreshold() {
        return _operator.getThreadCreationThreshold();
    }

    /**
     * Sets the threshold of queue processing thread creation
     * @param value threshold
     */
    public static void setQueueProcessingThreadCreationThreshold(int value) {

        if (StoredStates.set(Parallel.class, "queue-processing-thread-creation-threshold", value)) {
            _operator.setThreadCreationThreshold(value);
        }
    }

    /**
     * Gets the threshold of queue processing thread termination
     * @return threshold
     */
    public static int getQueueProcessingThreadTerminationThreshold() {
        return _operator.getThreadTerminationThreshold();
    }

    /**
     * Sets the threshold of queue processing thread termination
     * @param value threshold
     */
    public static void setQueueProcessingThreadTerminationThreshold(int value) {

        if (StoredStates.set(Parallel.class, "queue-processing-thread-termination-threshold",value)) {
            _operator.setThreadTerminationThreshold(value);
        }
    }

    /**
     * Gets the number of pending tasks
     * @return number of tasks
     */
    public static int getPendingTaskCount() {
        return _operator.getPendingTaskCount();
    }

    /**
     * Gets the number of waiting tasks
     * @return number of tasks
     */
    public static int getWaitingTaskCount() {
        return _operator.getWaitingTaskCount();
    }

    /**
     * Gets the number of running processing thread
     * @return number of threads
     */
    public static int getProcessingThreadCount() {
        return _operator.getThreadCount();
    }

    /**
     * Queues a tasks to be executed after a specified time
     * @param task task to be executed
     * @param state object to be passed to the task
     * @param invocationStamp time, after which the task will be executed
     * @return object, provides information about the queued task
     *
     * @exception InterruptedException throws if the calling thread is interrupted be for the
     * queueing operation is completed
     */
    public static TaskState queue(Task task, Object state, long invocationStamp)  throws InterruptedException {
        return _operator.queue(task, state, invocationStamp);
    }

    /**
     * Queues a task to be executed as soon as possible
     * @param task task to be executed
     * @param state object to be passed to the task
     * @return object, provides information about the queued task
     *
     * @exception InterruptedException throws if the calling thread is interrupted be for the
     * queueing operation is completed
     */
    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static TaskState queue(Task task, Object state) throws InterruptedException {
        return _operator.queue(task, state);
    }

    /**
     * Queues a tasks to be executed after a specified time
     * @param task task to be executed
     * @param invocationStamp time, after which the task will be executed
     * @return object, provides information about the queued task
     *
     * @exception InterruptedException throws if the calling thread is interrupted be for the
     * queueing operation is completed
     */
    @SuppressWarnings("UnusedReturnValue")
    public static TaskState queue(Action task, long invocationStamp) throws InterruptedException {
        return _operator.queue(task, invocationStamp);
    }

    /**
     * Queues a task to be executed as soon as possible
     * @param task task to be executed
     * @return object, provides information about the queued task
     *
     * @exception InterruptedException throws if the calling thread is interrupted be for the
     * queueing operation is completed
     */
    @SuppressWarnings("UnusedReturnValue")
    public static TaskState queue(Action task) throws InterruptedException {
        return  _operator.queue(task);
    }

    /**
     * Executes a task on separated thread immediately
     * @param task task to be executed
     * @param state object to be passed to the task
     * @param repeat indicates whether task execution should be repeated or not. If this parameter
     *               is set to {@code true} the task will be executed repeatedly.
     * @return object, provides information about the task
     */
    @SuppressWarnings("UnusedReturnValue")
    public static TaskState start(Task task, Object state, boolean repeat) {
        return _worker.start(task, state, repeat);
    }

    /**
     * Executes a task on a separated thread immediately
     * @param task to be executed
     * @return object, provides information about the task
     */
    public static TaskState start(Action task) {

        if (task == null) throw new IllegalArgumentException();

        return _worker.start(state -> ((Action)state).run(), task, false);
    }

    /**
     * Executes a for loop in which iterations run in parallel
     * @param fromInclusive the start index, inclusive
     * @param toExclusive the end index, exclusive
     * @param body the task that is invoked once per iteration.
     * @throws InterruptedException throws if the calling thread is interrupted before the loop
     * completed
     */
    public static void loop(int fromInclusive, int toExclusive, Action1<Integer> body) throws InterruptedException {

        final LoopCounter counter = singleton.new LoopCounter();

        int step = fromInclusive < toExclusive ? 1 : -1;

        for(int i = fromInclusive; step > 0 ? i < toExclusive : i > toExclusive; i+=step) {

            int index = i;

            queue(() -> {
                try {
                    counter.increase();
                    body.run(index);
                } finally {
                    counter.decrease();
                }

            });
        }

        while (counter.isWaiting()) Parallel.sleep();
    }


    /**
     * Executes a foreach operation on a collection in which iterations run in parallel
     * @param collection enumerable data source
     * @param body the task that is invoked once per iteration.
     * @param <T> type of data in the collection
     * @throws InterruptedException throws if the calling thread is interrupted before the operation
     * is completed
     */
    public static <T> void each(Iterable<T> collection, final Action1<T> body) throws InterruptedException {

        final LoopCounter counter = singleton.new LoopCounter();

        for (T one : collection){
            queue(state -> {
                try {
                    counter.increase();
                    //noinspection unchecked
                    body.run((T) state);
                } finally {
                    counter.decrease();
                }
            }, one);
        }

        while (counter.isWaiting()) Parallel.sleep();
    }

    /**
     * Indicates whether the calling thread is the main thread or not
     * @return true if the calling thread is the main thread; otherwise false
     */
    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds, subject to
     * the precision and accuracy of system timers and schedulers. The thread
     * does not lose ownership of any monitors.
     *
     * @param  millis
     *         the length of time to sleep in milliseconds
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative
    */
    public static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the number of milliseconds specified by {@link Workers} task sleep interval,
     * subject to the precision and accuracy of system timers and schedulers. The thread
     * does not lose ownership of any monitors.
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative
     */
    public static void sleep() throws InterruptedException {
        sleep(Workers.getTaskSleepInterval());
    }
}
