package shark.runtime;

import java.util.ArrayList;
import java.util.LinkedList;

import shark.Framework;
import shark.delegates.Action;
import shark.utils.Log;

/**
 * Executes tasks and optimizes usage of threads for task execution.
 */
@SuppressWarnings("WeakerAccess")
public class Operator {

    private class TaskInfo {
        TaskState state;
        long stamp;
    }

    /**
     * Create an operator
     * @param maxNumberOfThreads maximum number of threads allowed to be created
     * @param threadCreationThreshold threshold of thread creation
     * @param threadTerminationThreshold threshold of thread termination
     */
    public Operator(int maxNumberOfThreads, int threadCreationThreshold, int threadTerminationThreshold) {

        setMaxNumberOfThreads(maxNumberOfThreads);
        setThreadCreationThreshold(threadCreationThreshold);
        setThreadTerminationThreshold(threadTerminationThreshold);
    }

    class OperatorWorker extends Worker {

        private final ArrayList<TaskInfo> pendingQueue = new ArrayList<>();

        private final LinkedList<TaskState> waitingQueue = new LinkedList<>();

        private int threadCreationThreshold = 20;
        private int threadTerminationThreshold = 100;
        private int maxNumberOfThreads = 2;
        private boolean isMainThreadRunning = false;

        @Override
        protected void initialise() {
            isMainThreadRunning = true;
            registerTask(state -> _operator(), null, true);
        }

        private void  _operator() throws InterruptedException {

            TaskInfo[] tasks;

            int threadCreationPoint = 0;
            int threadTerminationPoint = 0;

            int lastCount = 0;

            while (isRunning() && !isStopping()) {

                synchronized (pendingQueue) {
                    tasks = pendingQueue.toArray(new TaskInfo[0]);
                    pendingQueue.clear();
                }

                long now = System.currentTimeMillis();

                for (final TaskInfo info : tasks) {
                    if (info.stamp > now) {

                        synchronized (pendingQueue) {
                            pendingQueue.add(info);
                        }
                    } else {

                        synchronized (waitingQueue) {
                            waitingQueue.add(info.state);
                        }
                    }
                }

                int count;
                synchronized (waitingQueue) {
                    count = waitingQueue.size();
                }

                if (count >= lastCount && taskCount() - 1 < maxNumberOfThreads) {
                    if (taskCount() == 1 || ++threadCreationPoint >= threadCreationThreshold) {
                        registerTask(state -> _processor(), null, true);
                        threadCreationPoint = 0;
                    }
                } else {
                    threadCreationPoint = 0;
                }

                lastCount = count;

                if (count + tasks.length == 0 && taskCount() == 1) {
                    if (++threadTerminationPoint >= threadTerminationThreshold) {

                        synchronized (pendingQueue) {
                            synchronized (waitingQueue) {
                                if (pendingQueue.size() + waitingQueue.size() == 0) {
                                    isMainThreadRunning = false;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    threadTerminationPoint = 0;
                }

                Parallel.sleep();
            }
        }

        private void _processor() {

            int threadTerminationPoint = 0;

            while (isRunning() && !isStopping())
            {
                TaskState info;
                synchronized (waitingQueue) {
                    info = waitingQueue.size() > 0 ? waitingQueue.pop() : null;
                }

                if (info == null)
                {
                    try {
                        Parallel.sleep();
                    }
                    catch (InterruptedException e) {
                        break;
                    }

                    if (++threadTerminationPoint >= threadTerminationThreshold) break;
                }
                else
                {
                    threadTerminationPoint = 0;
                    _runTask(info);
                }
            }
        }

        private void _runTask(TaskState info) {

            try {

                info._notifyStart(Thread.currentThread());
                info._getTask().run(info.getState());
                info._notifySuccess();
            }
            catch (Exception e) {

                //noinspection ConstantConditions
                if (!InterruptedException.class.isAssignableFrom(e.getClass()) && Framework.log) {
                    Log.error(Operator.class,
                            "Error detected while processing task",
                            "Class: " + info._getTask().getClass().getName(),
                            "Error: " + e.getMessage(),
                            Log.stringify(e.getStackTrace()));
                }

                info.notifyFailure(e);
            }
        }
    }

    private final OperatorWorker _worker = new OperatorWorker();

    /**
     * Gets threshold of thread creation
     * @return threshold
     */
    public int getThreadCreationThreshold() {
        return _worker.threadCreationThreshold;
    }

    /**
     * Sets threshold of thread creation
     * @param value threshold
     */
    public void setThreadCreationThreshold(int value) {
        if (value < 0) throw new IllegalArgumentException();
        _worker.threadCreationThreshold = value;
    }

    /**
     * Gets threshold of thread termination
     * @return threshold
     */
    public int getThreadTerminationThreshold() {
        return _worker.threadTerminationThreshold;
    }

    /**
     * Sets threshold of thread termination
     * @param value threshold
     */
    public void setThreadTerminationThreshold(int value) {
        if (value < 0) throw new IllegalArgumentException();
        _worker.threadTerminationThreshold = value;
    }

    /**
     * Gets maximum number of threads allowed to be created
     * @return number of threads
     */
    public int getMaxNumberOfThreads() {
        return _worker.maxNumberOfThreads;
    }

    /**
     * Sets maximum number of threads allowed to be created
     * @param value number of threads
     */
    public void setMaxNumberOfThreads(int value) {
        if (value < 0) throw new IllegalArgumentException();
        _worker.maxNumberOfThreads = value;
    }

    /**
     * Gets the number of running threads
     * @return number of threads
     */
    public int getThreadCount() {
        return Math.max(0, _worker.taskCount() - 1);
    }

    /**
     * Gets the number of tasks, not yet reached execution time
     * @return number of tasks
     */
    public int getPendingTaskCount() {

        synchronized (_worker.pendingQueue) {
            return _worker.pendingQueue.size();
        }
    }

    /**
     * Gets the number of tasks, waiting to be executed
     * @return number of tasks
     */
    public int getWaitingTaskCount() {

        synchronized (_worker.waitingQueue) {
            return _worker.waitingQueue.size();
        }
    }

    /**
     * Gets the number of all queued tasks
     * @return number of tasks
     */
    public int getTaskCount() {
        return getPendingTaskCount() + getWaitingTaskCount();
    }

    /**
     * Queues a task to be executed after a specified time
     * @param task task to be executed
     * @param state object to be passed to the task
     * @param invocationStamp time, after which the task should be executed
     * @return object, provides information about the task execution
     */
    public TaskState queue(Task task, Object state, long invocationStamp) throws InterruptedException {

        if (task == null) throw new IllegalArgumentException("task");

        final TaskInfo info = new TaskInfo();
        info.state = new TaskState(task, state, false);
        info.stamp = invocationStamp;

        boolean isMainThreadRunning;

        synchronized (_worker.pendingQueue) {

            _worker.pendingQueue.add(info);
            isMainThreadRunning = _worker.isMainThreadRunning;
        }


        while (!isMainThreadRunning && _worker.isRunning()) Parallel.sleep();
        if (!_worker.isRunning()) _worker.start();


        return info.state;
    }

    /**
     * Queues a task to be executed as soon as possible
     * @param task task to be executed
     * @param state object to be passed to the task
     * @return object, provides information about task execution
     *
     * @exception InterruptedException throws if the calling thread is interrupted be for the
     * queueing operation is completed
     */
    public TaskState queue(Task task, Object state) throws InterruptedException {

        if (task == null) throw new IllegalArgumentException("task");

        final TaskState info = new TaskState(task, state, false);

        boolean isMainThreadRunning;

        synchronized (_worker.waitingQueue) {
            _worker.waitingQueue.add(info);
            isMainThreadRunning = _worker.isMainThreadRunning;
        }

        while (!isMainThreadRunning && _worker.isRunning()) Parallel.sleep();
        if (!_worker.isRunning()) _worker.start();

        return info;
    }

    /**
     * Queues a task to be executed as soon as possible
     * @param task task to be executed
     * @return object, provides information about the task execution
     *
     * @exception InterruptedException throws if the calling thread is interrupted be for the
     * queueing operation is completed
     */
    public TaskState queue(Action task) throws InterruptedException {

        if (task == null) throw new IllegalArgumentException();

        return queue(state -> ((Action)state).run(), task);
    }

    /**
     * Queues a task to be executed after a specified time
     * @param task task to be executed
     * @param invocationStamp time, after which the task should be executed
     * @return object, provides information about the task execution
     *
     * @exception InterruptedException throws if the calling thread is interrupted be for the
     * queueing operation is completed
     */
    public TaskState queue(Action task, long invocationStamp) throws InterruptedException {

        if (task == null) throw new IllegalArgumentException();

        return queue(state -> ((Action)state).run(), task, invocationStamp);
    }
}
