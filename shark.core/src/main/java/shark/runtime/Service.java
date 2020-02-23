package shark.runtime;

import java.util.HashMap;

import shark.Framework;
import shark.components.IServiceHandler;
import shark.components.IServiceRequestInfo;
import shark.components.NotProceedServiceException;
import shark.components.ServiceException;
import shark.utils.Log;

public final class Service {

    private class _ExecutionInfo
    {
        public Service service;
        public IServiceRequestInfo request;
        public int requestIdentifier;
        public ServiceExecutionState state;
        public boolean sync;

        public _ExecutionInfo(Service service, IServiceRequestInfo request, int requestIdentifier, ServiceExecutionState state, boolean sync) {
            this.service = service;
            this.request = request;
            this.requestIdentifier = requestIdentifier;
            this.state = state;
            this.sync = sync;
        }
    }

    private int id;
    private IServiceHandler handler;
    private String name;
    private String[] alts;

    private HashMap<Integer, ServiceExecutionState> _managedExecutionStates = new HashMap<>();

    private long invocationCount = 0;
    private long executionCount = 0;
    private long totalExecutionTime = 0;

    Service(int id, String name, String[] alts, IServiceHandler handler){

        this.id = id;
        this.handler = handler;
        this.name = name;
        this.alts = alts;
    }

    public boolean is(Class<? extends IServiceHandler> test){

        return test.isAssignableFrom(handler.getClass());
    }

    public Class<? extends IServiceHandler> getHandlerClass() {

        return handler.getClass();
    }

    public Class<?> getDataClass() {
        return handler.getDataClass();
    }

    public Class<?> getReturnClass() {
        return handler.getReturnClass();
    }

    public String getName(){
        return name;
    }

    public int getId() {
        return id;
    }

    public String[] getAlternativeNames() {
        return alts;
    }

    public long getInvocationCount() {
        return invocationCount;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public long getAverageExecutionTime() {
        return executionCount > 0 ? totalExecutionTime / executionCount : 0;
    }

    private static void _executeTask(final _ExecutionInfo info) {

        long start = System.currentTimeMillis();

        try {
            info.service.executionCount++;
            info.state.notifyStart();

            Object response = info.service.handler.process(info.request);
            info.state.notifySuccess(response);
        }
        catch (Exception e){

            if (Framework.log) Log.error(Service.class,
                    "Error detected while executing service",
                    "Service: " + info.service.getName(),
                    "Error: " + e.getMessage(),
                    Log.stringify(e.getStackTrace()));

            info.state.notifyFailure(e);
        }
        finally {
            info.service.totalExecutionTime += System.currentTimeMillis() - start;

            final int wait = info.service.handler.determineResponseCachingTime(info.request, info.state.getResponse());

            if (info.state.isSucceed() && wait > 0) {
                if (info.sync) {
                    Parallel.start(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.currentThread().join(wait);
                            }
                            catch (InterruptedException e){
                            }
                            finally {

                                synchronized (info.service._managedExecutionStates) {
                                    info.service._managedExecutionStates.remove(info.requestIdentifier);
                                }
                            }
                        }
                    });
                }
                else {

                    try {
                        Thread.currentThread().join(wait);
                    }
                    catch (InterruptedException e){
                    }
                    finally {

                        synchronized (info.service._managedExecutionStates) {
                            info.service._managedExecutionStates.remove(info.requestIdentifier);
                        }
                    }
                }
            }
            else {

                synchronized (info.service._managedExecutionStates) {
                    info.service._managedExecutionStates.remove(info.requestIdentifier);
                }
            }
        }
    }

    public ServiceExecutionResult process(final IServiceRequestInfo request) throws InterruptedException {

        ServiceExecutionState result = null;
        invocationCount++;

        int requestId = 0;

        try {
            requestId = handler.computeIdentifier(request);
        }
        catch (Exception e) {

            result = new ServiceExecutionState(request);
            result._notifyFailure(ServiceException.class.isAssignableFrom(e.getClass()) ? e : new NotProceedServiceException("Error detected while computing request identifier", e));
        }
        finally {

            if (result == null){

                final int id = requestId;

                boolean execute;

                synchronized (_managedExecutionStates) {

                    execute = !_managedExecutionStates.containsKey(id);
                    if (execute) _managedExecutionStates.put(id, new ServiceExecutionState(request));
                    result = _managedExecutionStates.get(id);
                }

                if (execute) _executeTask(new _ExecutionInfo(this, request, requestId, result, true));
            }
        }

        while (!result.isCompleted()) Thread.currentThread().join(Workers.getTaskSleepInterval());

        return new ServiceExecutionResult(result.getResponse(), result.getRequest());
    }

    public ServiceExecutionState processAsync(final IServiceRequestInfo request){

        invocationCount++;

        ServiceExecutionState result = null;
        int requestId = 0;

        try{
            requestId = handler.computeIdentifier(request);
        }
        catch (Exception e) {

            result = new ServiceExecutionState(request);
            result._notifyFailure(ServiceException.class.isAssignableFrom(e.getClass()) ? e : new NotProceedServiceException("Error detected while computing request identifier", e));
        }
        finally {

            if (result == null) {

                final int id = requestId;

                boolean execute;
                synchronized (_managedExecutionStates) {
                    execute = !_managedExecutionStates.containsKey(id);
                    if (execute) _managedExecutionStates.put(id, new ServiceExecutionState(request));
                    result = _managedExecutionStates.get(id);
                }

                if (execute) Parallel.queue(new Task() {
                    @Override
                    public void run(Object state) {
                        _executeTask((_ExecutionInfo)state);
                    }
                }, new _ExecutionInfo(this, request, requestId, result, false));
            }
        }

        return result;
    }
}
