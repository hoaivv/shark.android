package shark.runtime;

class ParallelWorker extends Worker {

    public TaskState start(Task task, Object state, boolean repeat) {
        if (task == null) throw new IllegalArgumentException("task");

        TaskState result = null;

        if (task != null) {
            result = registerTask(task, state, repeat);
            if (result.isWaiting()) start();
        }

        return result;
    }
}