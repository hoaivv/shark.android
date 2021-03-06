package shark.runtime;

public class TaskState extends AsyncOperationState {

    private Thread thread = null;
    private final Task task;
    private final Object state;

    Thread getThread(){
        return thread;
    }

    Task _getTask() {
        return task;
    }

    public Object getState() {
        return state;
    }

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    public boolean isRepeatable;

    public boolean isRunning() {
        return !isWaiting() && !isCompleted();
    }

    TaskState(Task task, Object state, boolean repeat) {

        this.task = task;
        this.state = state;
        this.isRepeatable = repeat;
    }

    void _notifyStart(Thread thread) {

        this.thread = thread;
        notifyStart();
    }

    void _notifySuccess(){
        notifySuccess(null);
    }

    void _notifyFailure(Exception e) {
        notifyFailure(e);
    }
}
