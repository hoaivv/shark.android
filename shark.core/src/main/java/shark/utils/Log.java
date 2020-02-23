package shark.utils;

import java.util.LinkedList;

import shark.Framework;

/**
 * Provides easy access to Shark Framework Logging System
 */
public final class Log {

    private Log(){
    }

    private static LinkedList<LogData> _logs = new LinkedList<>();

    private static Thread _thread = null;

    private static Runnable _threadHandler = new Runnable() {
        @Override
        public void run() {
            try {

                int threadTerminationPoint = 0;

                while (_thread != null) {

                    Object[] logs;

                    synchronized (_logs) {

                        logs = _logs.toArray();
                        if (logs.length == 0) {

                            if (++threadTerminationPoint >= 100) {
                                _thread = null;
                                return;
                            }
                        }
                        else {

                            threadTerminationPoint = 0;
                            _logs.clear();
                        }
                    }

                    for (Object one : logs) {
                        LogData log = (LogData) one;
                        Framework.log(log);
                    }

                    Thread.currentThread().join(10);
                }
            }
            catch (InterruptedException e){
            }
        }
    };

    /**
     * Writes a log via Shark Framework Logging System
     * @param owner owner of the log
     * @param type type of the log
     * @param skipTrace number of stack trace to be skipped when generating trace information of the log.
     *                  This value will have no effects if {@link Framework#traceLogCaller} set to false
     * @param messages messages of the log
     */
    public static void write(Class<?> owner, LogType type, int skipTrace, String... messages) {

        if (!Framework.log) return;

        String message = "";
        for (String one : messages)
            message += one + (one == messages[messages.length - 1] ? "" : "\r\n");

        LogData log;

        if (Framework.traceLogCaller) {

            StackTraceElement[] traces = Thread.currentThread().getStackTrace();
            StackTraceElement trace = traces.length > 0 ? traces[Math.max(0, Math.min(skipTrace + 3, traces.length - 1))] : null;

            log = new LogData(owner, trace == null ? null : trace.getClassName() + "." + trace.getMethodName(), type, message);
        } else {
            log = new LogData(owner, null, type, message);
        }

        synchronized (_logs) {

            _logs.add(log);

            if (_thread == null) {
                _thread = new Thread(_threadHandler);
                _thread.setDaemon(true);
                _thread.start();
            }
        }
    }

    /**
     * Writes a log via Shark Framework Logging System
     * @param owner owner of the log
     * @param type type of the log
     *                  This value will have no effects if {@link Framework#traceLogCaller} set to false
     * @param messages messages of the log
     */
    public static void write(Class<?> owner, LogType type, String... messages){
        write(owner, type, 1, messages);
    }

    /**
     * Writes an information log via Shark Framework Loggin System
     * @param owner owner of the log
     * @param messages messages of the log
     */
    public static void information(Class<?> owner, String... messages) {
        write(owner, LogType.Information, 1, messages);
    }

    /**
     * Writes an information log via Shark Framework Loggin System
     * @param owner owner of the log
     * @param skipTrace number of stack trace to be skipped when generating trace information of the log.
     *                  This value will have no effects if {@link Framework#traceLogCaller} set to false
     * @param messages messages of the log
     */
    public static void information(Class<?> owner, int skipTrace, String... messages) {
        write(owner, LogType.Information, skipTrace + 1, messages);
    }

    /**
     * Writes a warning log via Shark Framework Loggin System
     * @param owner owner of the log
     * @param messages messages of the log
     */
    public static void warning(Class<?> owner, String... messages) {
        write(owner, LogType.Warning, 1, messages);
    }

    /**
     * Writes a warning log via Shark Framework Loggin System
     * @param owner owner of the log
     * @param skipTrace number of stack trace to be skipped when generating trace information of the log.
     *                  This value will have no effects if {@link Framework#traceLogCaller} set to false
     * @param messages messages of the log
     */
    public static void warning(Class<?> owner, int skipTrace, String... messages) {
        write(owner, LogType.Warning, skipTrace + 1, messages);
    }

    /**
     * Writes an error log via Shark Framework Loggin System
     * @param owner owner of the log
     * @param messages messages of the log
     */
    public static void error(Class<?> owner, String... messages) {
        write(owner, LogType.Error, 1, messages);
    }

    /**
     * Writes an error log via Shark Framework Loggin System
     * @param owner owner of the log
     * @param skipTrace number of stack trace to be skipped when generating trace information of the log.
     *                  This value will have no effects if {@link Framework#traceLogCaller} set to false
     * @param messages messages of the log
     */
    public static void error(Class<?> owner, int skipTrace, String... messages) {
        write(owner, LogType.Error, skipTrace + 1, messages);
    }

    /**
     * Represents elements of an array as a string, each line of which represent a collection element.
     * @param collection collection to be represented as a string
     * @param <T> typeof the array
     * @return A string represent the provided array
     */
    public static <T> String stringify(T[] collection) {

        String result = "";

        if (collection == null) return  result;

        for (int i = 0; i < collection.length; i++) {
            result += collection[i] == null ? "" : collection[i].toString() + (i == collection.length-1 ? "" : "\r\n");
        }

        return result;
    }
}
