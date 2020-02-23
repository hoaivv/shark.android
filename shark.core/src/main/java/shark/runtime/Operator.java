package shark.runtime;

import java.util.ArrayList;
import java.util.LinkedList;

import shark.Framework;
import shark.utils.Log;

public class Operator {

    class TaskInfo {
        public TaskState state;
        public long stamp;
    }

    public Operator(int maxNumberOfThreads, int threadCreationThreshold, int threadTerminationThreshold) {

        setMaxNumberOfThreads(maxNumberOfThreads);
        setThreadCreationThreshold(threadCreationThreshold);
        setThreadTerminationThreshold(threadTerminationThreshold);
    }

    public Operator(){
    }

    class OperatorWorker extends Worker {

        public ArrayList<TaskInfo> pendingQueue = new ArrayList<>();

        public LinkedList<TaskState> waitingQueue = new LinkedList<>();

        public int threadCreationThreshold = 20;
        public int threadTerminationThreshold = 100;
        public int maxNumberOfThreads = 2;
        public boolean isMainThreadRunning = false;

        @Override
        protected void initialise() {
            isMainThreadRunning = true;
            registerTask(_operator, null, true);
        }

        private Task _operator = new Task() {
            @Override
            public void run(Object state) {

                TaskInfo[] infos;

                int threadCreationPoint = 0;
                int threadTerminationPoint = 0;

                int lastCount = 0;

                while (isRunning() && !isStopping()){

                    synchronized (pendingQueue) {
                        infos = pendingQueue.toArray(new TaskInfo[0]);
                        pendingQueue.clear();
                    }

                    long now = System.currentTimeMillis();

                    for(final TaskInfo info : infos) {
                        if (info.stamp > now) {

                            synchronized (pendingQueue) {
                                pendingQueue.add(info);
                            }
                        }
                        else {

                            synchronized (waitingQueue) {
                                waitingQueue.add(info.state);
                            }
                        }
                    }

                    int count;
                    synchronized (waitingQueue) {
                        count = waitingQueue.size();
                    }

                    if (count >= lastCount && getTaskCount() - 1 < maxNumberOfThreads) {
                        if (getTaskCount() == 1 || ++threadCreationPoint >= threadCreationThreshold) {
                            registerTask(_processor, null, true);
                            threadCreationPoint = 0;
                        }
                    }
                    else {
                        threadCreationPoint = 0;
                    }

                    lastCount = count;

                    if (count + infos.length == 0 && getTaskCount() == 1) {
                        if (++threadTerminationPoint >= threadTerminationPoint) {

                            synchronized (pendingQueue) {
                                synchronized (waitingQueue) {
                                    if (pendingQueue.size() + waitingQueue.size() == 0) {
                                        isMainThreadRunning = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    else {
                        threadTerminationPoint = 0;
                    }

                    try {
                        Thread.currentThread().join(Workers.getTaskSleepInterval());
                    }
                    catch (InterruptedException e){
                        break;
                    }
                }
            }
        };

        private Task _processor = new Task() {
            @Override
            public void run(Object state) {

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
                            Thread.currentThread().join(Workers.getTaskSleepInterval());
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
        };

        private void _runTask(TaskState info) {

            try {

                info._notifyStart(Thread.currentThread());
                info._getTask().run(info.getState());
                info._notifySuccess();
            }
            catch (Exception e) {

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

    private OperatorWorker _worker = new OperatorWorker();

    public int getThreadCreationThreshold() {
        return _worker.threadCreationThreshold;
    }

    public void setThreadCreationThreshold(int value) {
        if (value < 0) throw new IllegalArgumentException();
        _worker.threadCreationThreshold = value;
    }

    public int getThreadTerminationThreshold() {
        return _worker.threadTerminationThreshold;
    }

    public void setThreadTerminationThreshold(int value) {
        if (value < 0) throw new IllegalArgumentException();
        _worker.threadTerminationThreshold = value;
    }

    public int getMaxNumberOfThreads() {
        return _worker.maxNumberOfThreads;
    }

    public void setMaxNumberOfThreads(int value) {
        if (value < 0) throw new IllegalArgumentException();
        _worker.maxNumberOfThreads = value;
    }

    public int getThreadCount() {
        return Math.max(0, _worker.getTaskCount() - 1);
    }

    public int getPendingTaskCount() {

        synchronized (_worker.pendingQueue) {
            return _worker.pendingQueue.size();
        }
    }

    public int getWaitingTaskCount() {

        synchronized (_worker.waitingQueue) {
            return _worker.waitingQueue.size();
        }
    }

    public int getTaskCount() {
        return getPendingTaskCount() + getWaitingTaskCount();
    }

    public TaskState queue(Task task, Object state, long invocationStamp) {

        if (task == null) throw new IllegalArgumentException("task");

        final TaskInfo info = new TaskInfo();
        info.state = new TaskState(task, state, false);
        info.stamp = invocationStamp;

        boolean isMainThreadRunning;

        synchronized (_worker.pendingQueue) {

            _worker.pendingQueue.add(info);
            isMainThreadRunning = _worker.isMainThreadRunning;
        }

        try {
            while (!isMainThreadRunning && _worker.isRunning()) Thread.currentThread().join(10);
            if (!_worker.isRunning()) _worker.start();
        }
        catch (InterruptedException e){
        }

        return info.state;
    }

    public TaskState queue(Task task, Object state) {

        if (task == null) throw new IllegalArgumentException("task");

        final TaskState info = new TaskState(task, state, false);

        boolean isMainThreadRunning;

        synchronized (_worker.waitingQueue) {
            _worker.waitingQueue.add(info);
            isMainThreadRunning = _worker.isMainThreadRunning;
        }

        try {
            while (!isMainThreadRunning && _worker.isRunning()) Thread.currentThread().join(10);
            if (!_worker.isRunning()) _worker.start();
        }
        catch (InterruptedException e){
        }

        return info;
    }

    public TaskState queue(Runnable action) {

        if (action == null) throw new IllegalArgumentException();

        return queue(new Task() {
            @Override
            public void run(Object state) {
                ((Runnable)state).run();
            }
        }, action);
    }

    public TaskState queue(Runnable action, long invocationStamp) {

        if (action == null) throw new IllegalArgumentException();

        return queue(new Task() {
            @Override
            public void run(Object state) {
                ((Runnable)state).run();
            }
        }, action, invocationStamp);
    }
}
