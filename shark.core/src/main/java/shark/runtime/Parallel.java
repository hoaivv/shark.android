package shark.runtime;

public final class Parallel {

    private static ParallelWorker _worker = new ParallelWorker();
    private static Operator _operator = new Operator(100, 10, 50);

    private static Parallel signature = new Parallel();

    public static int getMaxNumberOfQueueProcessingThreads() {
        return _operator.getMaxNumberOfThreads();
    }

    public static void setMaxNumberOfQueueProcessingThreads(int value) {

        if (StoredStates.set(Parallel.class, "max-number-of-queue-processing-threads", value)) {
            _operator.setMaxNumberOfThreads(value);
        }
    }

    public static int getQueueProcessingThreadCreationThreshold() {
        return _operator.getThreadCreationThreshold();
    }

    public static void setQueueProcessingThreadCreationThreshold(int value) {

        if (StoredStates.set(Parallel.class, "queue-processing-thread-creation-threshold", value)) {
            _operator.setThreadCreationThreshold(value);
        }
    }

    public static int getQueueProcessingThreadTerminationThreshold() {
        return _operator.getThreadTerminationThreshold();
    }

    public static void setQueueProcessingThreadTerminationThreshold(int value) {

        if (StoredStates.set(Parallel.class, "queue-processing-thread-termination-threshold",value)) {
            _operator.setThreadTerminationThreshold(value);
        }
    }

    public static int getPendingTaskCount() {
        return _operator.getPendingTaskCount();
    }

    public static int getWaitingTaskCount() {
        return _operator.getWaitingTaskCount();
    }

    public static int getProcessingThreadCount() {
        return _operator.getThreadCount();
    }

    public static TaskState queue(Task task, Object state, long invocationStamp) {
        return _operator.queue(task, state, invocationStamp);
    }

    public static TaskState queue(Task task, Object state) {
        return _operator.queue(task, state);
    }

    public static TaskState queue(Runnable action, long invocationStamp) {
        return _operator.queue(action, invocationStamp);
    }

    public static TaskState queue(Runnable action) {
        return  _operator.queue(action);
    }

    public static TaskState start(Task task, Object state, boolean repeat) {
        return _worker.start(task, state, repeat);
    }

    public static TaskState start(Runnable action) {

        if (action == null) throw new IllegalArgumentException();

        return _worker.start(new Task() {
            @Override
            public void run(Object state) {
                ((Runnable)state).run();
            }
        }, action, false);
    }

    public static void loop(int fromInclusive, int toExclusive, final Action.One<Integer> body) throws InterruptedException {

        final ParallelLoopCounter counter = new ParallelLoopCounter();

        int step = fromInclusive < toExclusive ? 1 : -1;

        for(int i = fromInclusive; step > 0 ? i < toExclusive : i > toExclusive; i+=step) {

            final int index = i;
            queue(new Runnable() {
                @Override
                public void run() {
                    try {
                        counter.increase();
                        body.run(index);
                    }
                    finally {
                        counter.decrease();
                    }
                }
            });
        }

        while (!counter.isCompleted()) Thread.currentThread().join(Workers.getTaskSleepInterval());
    }

    public static <T> void each(Iterable<T> collection, final Action.One<T> body) throws InterruptedException {

        final ParallelLoopCounter counter = new ParallelLoopCounter();

        for (T one : collection){
            queue(new Task() {
                @Override
                public void run(Object state) {
                    try {
                        counter.increase();
                        body.run((T) state);
                    }
                    finally {
                        counter.decrease();
                    }
                }
            }, one);
        }

        while (!counter.isCompleted()) Thread.currentThread().join(Workers.getTaskSleepInterval());
    }
}
