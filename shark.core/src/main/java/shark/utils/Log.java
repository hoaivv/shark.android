package shark.utils;

import java.util.LinkedList;

import shark.Framework;

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

    public static void write(Class<?> owner, LogType type, String... messages){
        write(owner, type, 1, messages);
    }

    public static void information(Class<?> owner, String... messages) {
        write(owner, LogType.information, 1, messages);
    }

    public static void information(Class<?> owner, int skipTrace, String... messages) {
        write(owner, LogType.information, skipTrace + 1, messages);
    }

    public static void warning(Class<?> owner, String... messages) {
        write(owner, LogType.warning, 1, messages);
    }

    public static void warning(Class<?> owner, int skipTrace, String... messages) {
        write(owner, LogType.warning, skipTrace + 1, messages);
    }

    public static void error(Class<?> owner, String... messages) {
        write(owner, LogType.error, 1, messages);
    }

    public static void error(Class<?> owner, int skipTrace, String... messages) {
        write(owner, LogType.error, skipTrace + 1, messages);
    }

    public static <T> String stringify(T[] collection) {

        String result = "";

        if (collection == null) return  result;

        for (int i = 0; i < collection.length; i++) {
            result += collection[i] == null ? "" : collection[i].toString() + (i == collection.length-1 ? "" : "\r\n");
        }

        return result;
    }
}
