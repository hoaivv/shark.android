package shark.runtime;

import java.util.ArrayList;

public final class Workers {

    private Workers() {
    }

    private static final ArrayList<Worker> instantiatedWorkers = new ArrayList<>();

    private static long stopTimeout = 10000;
    private static long taskSleepInterval = 10; // for weak android devices this amount of sleeping time is reasonable

    @SuppressWarnings("WeakerAccess")
    public static long getTaskSleepInterval() {
        return taskSleepInterval;
    }

    public static void setTaskSleepInterval(long value) {
        value = Math.max(value, 1);
        if (StoredStates.set(Workers.class, "task-sleep-interval", value)) {
            taskSleepInterval = value;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static long getStopTimeout() {
        return stopTimeout;
    }

    public static void setStopTimeout(long value) {
        value = Math.max(value, 1);
        if (StoredStates.set(Workers.class, "stop-timeout", value)) {
            stopTimeout = value;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static Worker[] getAll() {

        synchronized (instantiatedWorkers) {
            return instantiatedWorkers.toArray(new Worker[0]);
        }
    }

    public static Worker[] getRunningWorkers() {

        ArrayList<Worker> results = new ArrayList<>();
        for(Worker one : getAll()) if (one.isRunning()) results.add(one);
        return results.toArray(new Worker[0]);
    }

    public static boolean hasRunningWorker() {

        for(Worker one : getAll()) if (one.isRunning()) return true;
        return false;
    }

    public static int getRunningWorkerCount() {

        int count = 0;
        for(Worker one : getAll()) if (one.isRunning()) count++;
        return count;
    }

    static void register(final Worker worker) {

        synchronized (instantiatedWorkers) {
            instantiatedWorkers.add(worker);
        }
    }
}
