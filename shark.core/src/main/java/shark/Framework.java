package shark;

import android.content.Context;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import shark.components.IAutomation;
import shark.components.ISharkComponent;
import shark.components.ServiceHandler;
import shark.io.File;
import shark.runtime.Automations;
import shark.runtime.Services;
import shark.runtime.Worker;
import shark.runtime.Workers;
import shark.runtime.events.ActionEvent;
import shark.runtime.events.ActionTrigger;
import shark.utils.Log;
import shark.utils.LogData;

/**
 * Controller of Shark Framework
 */
public final class Framework {

    private static boolean initialised;

    /**
     * Gets or sets a value which directs the framework to writes loges or not
     */
    public static boolean log;

    /**
     * Gets or sets a value which directs the framework to writes debug loges or not. This value
     * will have no effects if {@link #log} is set to false
     */
    public static boolean debug = false;

    /**
     * Gets or sets a value which directs the framework to trace log writer or not. This value
     * will have no effects if {@link #log} is set to false
     */
    public static boolean traceLogCaller = false;

    /**
     * Gets or sets a value which directs the framework to writes loges to files, This value will
     * have no effects if {@link #log} is set to false
     */
    public static boolean writeLogsToFiles = false;

    private static Framework signature = new Framework();

    private Framework(){
    }

    /**
     * Event to be triggered when a log is written using Shark Logging System
     */
    public static ActionEvent<LogData> onLogged = new ActionEvent<>(signature);

    /**
     * Event to be triggered when the framework is started
     */
    public static ActionTrigger onStarted = new ActionTrigger(signature);

    /**
     * Event to be triggered when the framework is stopped
     */
    public static ActionTrigger onStopped = new ActionTrigger(signature);

    /**
     * Indicates whether the framework is running or not. The framework is defined as running if
     * at least one {@link IAutomation} or {@link Worker} is running
     * @return true if the framework is running; otherwise false
     */
    public static boolean isRunning() {

        synchronized (signature) {
            return Automations.hasRunningAutomation() || Workers.hasRunningWorker();
        }
    }

    /**
     * Indicates whether the framework is initialised or not
     * @return true if the framework is initialised; otherwise false
     */
    public static boolean isInitialised(){
        return initialised;
    }

    private static String dataDirectory = null;

    /**
     * Gets data directory of the application where application generated files can be stored. This
     * method will block the calling thread untils the data directory information is avaialble to
     * the framework.
     * @return Information of the application data directory
     * @throws InterruptedException throws if the thread on which this method is being invoked is
     * interrupted
     */
    public static File getDataDirectory() throws InterruptedException {

        while (dataDirectory == null) Thread.currentThread().join(100);
        return new File(dataDirectory);
    }

    /**
     * Starts Shark.Framework and loading Shark's components. If this method is invoked on the main
     * thread it will run asynchronously, otherwise it will run synchronously.
     * @param context context provided at runtime by android
     * @param components component to be loaded before starting the framework
     */
    public static void start(Context context, ISharkComponent... components) {

        Runnable commit = () -> {

            synchronized (signature) {
                if (isRunning() && initialised) return;

                if (debug && log) Log.information(Framework.class, "Starting Shark");

                if (!initialised) {

                    initialised = true;

                    dataDirectory = context.getFilesDir().toString();

                    if (components.length > 0 && log && debug)
                        Log.information(Framework.class, "Registering Shark components");

                    for (ISharkComponent component : components) {

                        if (component == null) continue;

                        if (ServiceHandler.class.isAssignableFrom(component.getClass())) {
                            Services.register((ServiceHandler) component);
                        } else {
                            if (IAutomation.class.isAssignableFrom(component.getClass()))
                                Automations.register((IAutomation) component);
                        }
                    }
                }

                if (debug) Log.information(Framework.class, "Starting automations");

                for (IAutomation one : Automations.getAll()) {

                    if (one.isRunning()) continue;

                    if (Worker.class.isAssignableFrom(one.getClass())) {
                        ((Worker) one).start();
                    } else {
                        if (debug && log)
                            Log.information(Automations.class, "Starting an automation", "Automation: " + one);

                        try {
                            one.start();
                        } catch (Exception e) {

                            if (log) Log.error(Framework.class,
                                    "Error detected",
                                    "Operation: start an automation",
                                    "Automation: " + one,
                                    "Error: " + e.getMessage(),
                                    Log.stringify(e.getStackTrace()));

                            if (debug && log) Log.information(Automations.class,
                                    "An automation is " + (one.isRunning() ? "started" : "not started"),
                                    "Automation: " + one);
                        }
                    }
                }

                if (debug) Log.information(Framework.class, "Shark is started");

                try {
                    onStarted.invoke(signature);
                } catch (Exception e) {
                    if (log) Log.error(Framework.class,
                            "Error detected during invocation of event Framework.onStarted",
                            "Error: " + e.getMessage(),
                            Log.stringify(e.getStackTrace()));
                }
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {

            Thread thread = new Thread(commit);
            thread.setDaemon(true);
            thread.start();
        }
        else {
            commit.run();
        }
    }

    /**
     * Indicates whether loges of a specified owner is written to file or not
     * @param owner owner of the loges
     * @return true if loges of the specified owner is written to file and available to read; otherwise false
     */
    public static boolean hasLogs(Class<?> owner) {

        try {
            File file = new shark.io.File(getDataDirectory() + "/logs/" + owner.getName() + ".log");
            return file.exists();
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets {@link File} which provides access to loges of a specified owner
     * @param owner owner of the loges
     * @return instance of {@link File} if succeed; otherwise null
     */
    public static File getLogs(Class<?> owner) {

        try {
            File file = new shark.io.File(getDataDirectory() + "/logs/" + owner.getName() + ".log");
            return file.exists() ? file : null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Deletes loges of a specified owner
     * @param owner owner of the loges
     * @return true if succeed; otherwise false;
     */
    public static boolean deleteLogs(Class<?> owner) {

        try {
            File file = new shark.io.File(getDataDirectory() + "/logs/" + owner.getName() + ".log");
            return !file.exists() || file.delete();
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Delete all loges
     * @return true if succeed; otherwise false
     */
    public static boolean deleteLogs() {

        try {
            File path = new shark.io.File(getDataDirectory() + "/logs");
            if (!path.exists() || !path.isDirectory()) return true;

            for (java.io.File file : path.listFiles())
                if (file.isFile() && !file.delete()) return false;
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Writes a log. This method will have no effects if {@link #log} set to false
     * @param data log data
     * @return true if succeed; otherwise false
     */
    public static boolean log(LogData data) {

        if (!log) return false;

        if (!onLogged.hasHandler() && !writeLogsToFiles) return false;

        try { onLogged.invoke(signature, data); } catch (Exception e) { };

        if (!writeLogsToFiles) return true;

        try {

            File path = new File(getDataDirectory() + "/logs");
            if ((!path.exists() || !path.isDirectory()) && !path.mkdir()) return false;

            path = new shark.io.File(getDataDirectory() + "/logs/" + data.getOwner().getName() + ".log");
            path.appendAllText("[" + new Date().toString() + "] " + data.getType() + "\r\n" + data.getTrace() + "\r\n" + data.getMessage() + "\r\n");

            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    /**
     * Stops the framework and all of its running components
     */
    public static void stop() {

        synchronized (signature) {

            if (!isRunning()) return;

            if (debug && log) Log.information(Framework.class,
                    "Stopping Shark");

            if (debug && log) Log.information(Framework.class,
                    "Stopping automations and workers");

            IAutomation[] allAutomations = Automations.getRunningAutomations();

            ArrayList<Thread> componentStoppingThreads = new ArrayList<>();

            for (IAutomation automation : allAutomations) {

                Thread T = new Thread(_componentStoppingThreadHandler(automation));
                componentStoppingThreads.add(T);
                T.start();
            }

            Worker[] allWorkers = Workers.getRunningWorkers();

            for (Worker worker : allWorkers) {
                Thread T = new Thread(_componentStoppingThreadHandler(worker));
                componentStoppingThreads.add(T);
                T.start();
            }

            for (Thread T : componentStoppingThreads) {

                try {
                    T.join();
                } catch (InterruptedException e) {
                }
            }

            if (log) {
                if (isRunning()) {
                    HashSet<String> running = new HashSet<>();

                    for (IAutomation automation : Automations.getRunningAutomations())
                        running.add(automation.getClass().getPackage().getName() + "/" + automation.getClass().getName() + " (automation)");
                    for (Worker worker : Workers.getRunningWorkers())
                        if (!Automations.isRegistered(worker))
                            running.add(worker.getClass().getPackage().getName() + "/" + worker.getClass().getName() + " (worker)");

                    if (log) Log.warning(Framework.class,
                            "Shark is not stopped, the following components is still running", Log.stringify(running.toArray()));
                } else {
                    if (log) Log.information(Framework.class, "Shark is stopped");
                }
            }

            try {
                onStopped.invoke(signature);
            } catch (Exception e) {
                if (log) Log.error(Framework.class,
                        "Error detected during invocation of event Framework.onStopped",
                        "Error: " + e.getMessage(),
                        Log.stringify(e.getStackTrace()));
            }
        }
    }

    private static Runnable _componentStoppingThreadHandler(final Object component) {

        return new Runnable() {
            @Override
            public void run() {

                if (Worker.class.isAssignableFrom(component.getClass())) {
                    ((Worker)component).stop();
                }
                else {
                    if (Framework.debug) Log.information(Automations.class,
                            "Stopping automation",
                            "Class: " + component.getClass().getName(),
                            "Package: " + component.getClass().getPackage().getName());
                }

                try {
                    ((IAutomation)component).stop();
                }
                catch (Exception e) {
                    Log.error(Automations.class,
                            "Error detected",
                            "Operation: stopping automation",
                            "Class: " + component.getClass().getName(),
                            "Package: " + component.getClass().getPackage().getName(),
                            "Error: " + e.getMessage(),
                            Log.stringify(e.getStackTrace()));
                }
            }
        };
    }
}
