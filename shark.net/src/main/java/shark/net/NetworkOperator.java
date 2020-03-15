package shark.net;

import java.util.HashSet;

import shark.delegates.Action;
import shark.runtime.Operator;
import shark.runtime.StoredStates;

@SuppressWarnings("WeakerAccess")
public class NetworkOperator {

    private static final Operator operatorPR = new Operator(StoredStates.getInt(NetworkOperator.class, "max-number-of-processor-threads", 100), StoredStates.getInt(NetworkOperator.class, "thread-creation-threshold", 20), StoredStates.getInt(NetworkOperator.class, "thread-termination-threshold", 100));
    private static final Operator operatorCK = new Operator(StoredStates.getInt(NetworkOperator.class, "max-number-of-checker-threads", 10), StoredStates.getInt(NetworkOperator.class, "thread-creation-threshold", 20), StoredStates.getInt(NetworkOperator.class, "thread-termination-threshold", 100));
    private static final Operator operatorIO = new Operator(StoredStates.getInt(NetworkOperator.class, "max-number-of-operator-threads", 100), StoredStates.getInt(NetworkOperator.class, "thread-creation-threshold", 20), StoredStates.getInt(NetworkOperator.class, "thread-termination-threshold", 100));

    private static HashSet<Action> _Lookup = new HashSet<>();

    static void _enqueueIO(Action action) throws InterruptedException {
        operatorIO.queue(action);
    }

    static void _enqueueIO(Action action, long timeUtc) throws InterruptedException {
        operatorIO.queue(action, timeUtc);
    }

    static void _enqueueChecker(Action checker, long timeUtc) throws InterruptedException {
        operatorCK.queue(checker, timeUtc);
    }

    static void _enqueueProcessor(Action action) throws InterruptedException {
        operatorPR.queue(action);
    }

    public static int getThreadCreationThreshold(){
        return operatorPR.getThreadCreationThreshold();
    }

    public static void setThreadCreationThreshold(int value) {
        operatorPR.setThreadCreationThreshold(value);
        operatorIO.setThreadCreationThreshold(value);
        operatorCK.setThreadCreationThreshold(value);
        StoredStates.set(NetworkOperator.class, "thread-creation-threshold", operatorPR.getThreadCreationThreshold());
    }

    public static int getThreadTerminationThreshold(){
        return operatorPR.getThreadTerminationThreshold();
    }

    public static void setThreadTerminationThreshold(int value) {
        operatorPR.setThreadTerminationThreshold(value);
        operatorIO.setThreadTerminationThreshold(value);
        operatorCK.setThreadTerminationThreshold(value);
        StoredStates.set(NetworkOperator.class, "thread-termination-threshold", operatorPR.getThreadTerminationThreshold());
    }

    public static int getMaxNumberOfIOThreads() {
        return operatorIO.getMaxNumberOfThreads();
    }

    public static void setMaxNumberOfIOThreads(int value) {
        operatorIO.setMaxNumberOfThreads(value);
        StoredStates.set(NetworkOperator.class, "max-number-of-operator-threads", operatorIO.getMaxNumberOfThreads());
    }

    public static int getMaxNumberOfCheckerThreads() {
        return operatorCK.getMaxNumberOfThreads();
    }

    public static void setMaxNumberOfCheckerThreads(int value) {
        operatorCK.setMaxNumberOfThreads(value);
        StoredStates.set(NetworkOperator.class, "max-number-of-checker-threads", operatorCK.getMaxNumberOfThreads());
    }

    public static int getMaxNumberOfProcessorThreads() {
        return operatorPR.getMaxNumberOfThreads();
    }

    public static void setMaxNumberOfProcessorThreads(int value) {
        operatorPR.setMaxNumberOfThreads(value);
        StoredStates.set(NetworkOperator.class, "max-number-of-processor-threads", operatorPR.getMaxNumberOfThreads());
    }

    public static int getIOThreadCount() {
        return operatorIO.getThreadCount();
    }

    public static int getCheckerThreadCount() {
        return operatorCK.getThreadCount();
    }

    public static int getProcessorThreadCount() {
        return operatorPR.getThreadCount();
    }

    public static int getWaitingIOCount() {
        return operatorIO.getWaitingTaskCount();
    }

    public static int getPendingIOCount() {
        return operatorIO.getPendingTaskCount();
    }

    public static int getWaitingCheckerCount() {
        return operatorCK.getWaitingTaskCount();
    }

    public static int getPendingCheckerCount() {
        return operatorCK.getPendingTaskCount();
    }

    public static int getWaitingProcessorCount() {
        return operatorPR.getWaitingTaskCount();
    }

    public static int getPendingProcessorCount() {
        return operatorPR.getPendingTaskCount();
    }
}
