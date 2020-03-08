package shark.runtime;

import java.util.HashMap;

import shark.Framework;
import shark.components.IServiceHandler;
import shark.components.IServiceRequestInfo;
import shark.components.NotProceedServiceException;
import shark.components.ServiceException;
import shark.utils.Log;

/**
 * Provides access to a registered Shark Service
 */
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

    /**
     * Checks whether handler of the service is an instance of a specified class or not
     * @param test class to be checked
     * @return true if handler of the service is an instance of the specified class; otherwise false
     */
    public boolean is(Class<? extends IServiceHandler> test){

        return test.isAssignableFrom(handler.getClass());
    }

    /**
     * Gets class of the service handler
     * @return class of the service handler
     */
    public Class<? extends IServiceHandler> getHandlerClass() {

        return handler.getClass();
    }

    /**
     * Gets class of processable data of the service
     * @return
     */
    public Class<?> getDataClass() {
        return handler.getDataClass();
    }

    /**
     * Gets class of response data of the service
     * @return
     */
    public Class<?> getReturnClass() {
        return handler.getReturnClass();
    }

    /**
     * Gets registered name of the service
     * @return service name
     */
    public String getName(){
        return name;
    }

    /**
     * Gets registered identifier of the service
     * @return service identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Gets registered aliases of the service
     * @return service aliases
     */
    public String[] getAlternativeNames() {
        return alts;
    }

    /**
     * Gets the number of time the service is requested
     * @return number of time the service is requested
     */
    public long getInvocationCount() {
        return invocationCount;
    }

    /**
     * Gets the number of time the service is executed
     * @return number of time the service is executed
     */
    public long getExecutionCount() {
        return executionCount;
    }

    /**
     * Gets total execution time (in milliseconds) of the service
     * @return milliseconds
     */
    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }

    /**
     * Gets average execution time (in milliseconds) of the service
     * @return milliseconds
     */
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

                    Parallel.start(() -> {
                        try {
                            Thread.currentThread().join(wait);
                        } catch (InterruptedException e) {
                        } finally {

                            synchronized (info.service._managedExecutionStates) {
                                info.service._managedExecutionStates.remove(info.requestIdentifier);
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

    /**
     * Requests the service to process a request
     * @param request request to be proceed
     * @return object, provides information of committed request processing operation
     * @throws InterruptedException throws if the calling thread is interrupted before the operation
     * is completed
     */
    public ServiceExecutionResult process(IServiceRequestInfo request) throws InterruptedException {

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

    /**
     * Requests the service to process a request asynchronously
     * @param request request to be proceed
     * @return object, provides information about request processing operation
     */
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

                if (execute) Parallel.queue(state -> _executeTask((_ExecutionInfo)state), new _ExecutionInfo(this, request, requestId, result, false));
            }
        }

        return result;
    }
}
