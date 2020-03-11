package shark.runtime;

import java.util.ArrayList;
import java.util.HashSet;

import shark.Framework;
import shark.utils.Log;

public abstract class Worker {

    private final HashSet<TaskState> _tasks = new HashSet<>();

    private boolean isRunning;
    private boolean isStarting;
    private boolean isStopping;

    @SuppressWarnings("WeakerAccess")
    public Worker() {
        Workers.register(this);
    }

    @SuppressWarnings("WeakerAccess")
    public int taskCount() {

        synchronized (_tasks) {
            return _tasks.size();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isRunning() {
        return isRunning;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isStarting(){
        return isStarting;
    }

    @SuppressWarnings({"WeakerAccess", "BooleanMethodIsAlwaysInverted"})
    public boolean isStopping(){
        return isStopping;
    }

    public boolean isAutomation() {
        return Automations.isRegistered(this);
    }

    private Runnable _taskHandler(final TaskState info) {

        final Worker worker = this;

        return new Runnable() {
            @Override
            public void run() {
                try {
                    info._notifyStart(Thread.currentThread());

                    do {
                        info._getTask().run(info.getState());
                        if (info.isRepeatable && !isStopping) Parallel.sleep();

                    } while (info.isRepeatable && !isStopping);

                    info._notifySuccess();

                } catch (InterruptedException e) {
                    if (Framework.debug) //noinspection ConstantConditions
                        Log.warning(worker.getClass(),
                            "Task is aborted",
                            "Instance: " + worker,
                            "Task: " + info._getTask().getClass().getPackage().getName() + "/" + info._getTask().getClass().getName());

                    info._notifyFailure(e);
                }
                catch (Exception e) {
                    //noinspection ConstantConditions
                    Log.error(worker.getClass(),
                            "Error detected",
                            "Instance: " + this,
                            "Operation: Task processing",
                            "Task: " + info._getTask().getClass().getPackage().getName() + "/" + info._getTask().getClass().getName(),
                            "Error: " + e.getMessage(),
                            Log.stringify(e.getStackTrace()));
                }
                finally {

                    synchronized (_tasks) {
                        if (_tasks.size() == 0) {

                            try {
                                shutdown();
                            }
                            catch (Exception e) {
                                //noinspection ConstantConditions
                                Log.error(worker.getClass(),
                                        "Error detected",
                                        "Instance: " + this,
                                        "Operation: Shutting down",
                                        "Task: " + info._getTask().getClass().getPackage().getName() + "/" + info._getTask().getClass().getName(),
                                        "Error: " + e.getMessage(),
                                        Log.stringify(e.getStackTrace()));
                            }

                            isRunning = false;

                            if (Framework.debug) Log.information(worker.getClass(),
                                    "Instance is stopped",
                                    "Instance: " + this);
                        }
                    }
                }
            }
        };
    }

    @SuppressWarnings("WeakerAccess")
    protected TaskState registerTask(final Task task, final Object state, final boolean repeat) {

        if (task == null) throw new IllegalArgumentException("task");

        synchronized (this){
            synchronized (_tasks) {

                final TaskState info = new TaskState(task, state, repeat);
                boolean ok = false;

                try {
                    if (isRunning) {

                        Thread T = new Thread(() -> {
                            try {
                                info._notifyStart(Thread.currentThread());

                                do {
                                    info._getTask().run(info.getState());
                                    if (info.isRepeatable && !isStopping) Parallel.sleep();
                                } while (info.isRepeatable && !isStopping);

                                info._notifySuccess();
                            }
                            catch (InterruptedException e) {

                                info._notifyFailure(e);
                            }
                        });

                        T.setDaemon(true);
                        T.start();
                    }

                    ok = true;
                }
                catch (Exception e) {

                    info._notifyFailure(e);
                }
                finally {

                    if (ok) _tasks.add(info);
                }

                return info;
            }
        }
    }

    public final void start() {

        synchronized (this){
            synchronized (_tasks){
                if (isRunning || isStarting) return;

                try {

                    isStarting = true;

                    if (Framework.debug) Log.information(this.getClass(),
                            "Starting instance",
                            "Instance: " + this);

                    try {
                        initialise();
                    }
                    catch (Exception e) {

                        Log.error(this.getClass(),
                                "Error detected",
                                "Instance: " + this,
                                "Operation: initialising",
                                "Error: " + e.getMessage(),
                                Log.stringify(e.getStackTrace()));

                        return;
                    }

                    try {
                        startup();
                    }
                    catch (Exception e) {
                        Log.error(this.getClass(),
                                "Error detected",
                                "Instance: " + this,
                                "Operation: starting up",
                                "Error: " + e.getMessage(),
                                Log.stringify(e.getStackTrace()));
                    }

                    if (_tasks.size() == 0) {
                        if (Framework.debug) Log.warning(this.getClass(),
                                "Instance has not tasks",
                                "Instance: " + this);
                    }

                    for (TaskState info: _tasks) {

                        try {
                            Thread T = new Thread(_taskHandler(info));
                            T.setDaemon(true);
                            T.start();
                            isRunning = true;
                        }
                        catch (Exception e) {

                            Log.error(this.getClass(),
                                    "Error detected",
                                    "Instance: " + this,
                                    "Operation: running tasks",
                                    "Task: " + info._getTask().getClass().getName(),
                                    "Error: " + e.getMessage(),
                                    Log.stringify(e.getStackTrace()));
                        }
                    }

                }
                finally {
                    isStarting = false;
                }
            }
        }
    }

    public void stop() {

        synchronized (this) {
            try {
                if (!isRunning || isStopping) return;

                isStopping = true;

                if (Framework.debug) Log.information(this.getClass(),
                        "Stopping an instance",
                        "Instance: " + this);

                long anchor = System.currentTimeMillis();

                while (System.currentTimeMillis() - anchor < Workers.getStopTimeout() && isRunning) {

                    try {
                        Parallel.sleep();
                    }
                    catch (InterruptedException ignored) {
                    }
                }

                if (isRunning) {
                    TaskState[] tasks;

                    synchronized (_tasks) {
                        tasks = _tasks.toArray(new TaskState[0]);
                    }

                    for (TaskState info : tasks) {

                        try {
                            info.getThread().interrupt();
                        } catch (Exception ignored) {
                        }
                    }

                    for (TaskState info : tasks) {
                        try {
                            info.getThread().join();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    if (Framework.debug) {

                        ArrayList<String> running = new ArrayList<>();

                        synchronized (_tasks) {

                            if (_tasks.size() > 0) {
                                for (TaskState info : _tasks)
                                    running.add(info._getTask().getClass().getPackage() + "/" + info._getTask().getClass().getName());
                            }
                        }

                        if (Framework.debug) Log.warning(this.getClass(),
                                "Instance is not fully stopped",
                                "Instance: " + this,
                                "Tasks: " + Log.stringify(running.toArray(new String[0])));
                    }
                }
            }
            finally {
                isStopping = false;
            }
        }
    }

    @SuppressWarnings({"WeakerAccess", "EmptyMethod"})
    protected void initialise() {
    }

    @SuppressWarnings({"WeakerAccess", "EmptyMethod"})
    protected void startup() {
    }

    @SuppressWarnings({"WeakerAccess", "EmptyMethod"})
    protected void shutdown() {
    }
}
