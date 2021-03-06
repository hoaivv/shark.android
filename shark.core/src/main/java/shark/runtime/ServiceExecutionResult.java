package shark.runtime;

import shark.components.IServiceRequestInfo;

/**
 * Describes result of a completed Shark Service execution
 */
public final class ServiceExecutionResult {

    private final Object executionResult;
    private final IServiceRequestInfo proceedRequest;

    ServiceExecutionResult(Object executionResult, IServiceRequestInfo proceedRequest){
        this.executionResult = executionResult;
        this.proceedRequest = proceedRequest;
    }

    /**
     * Gets and casts result of the executed service to a specified type
     * @param <T> type, to which the result to be casted
     * @return result of the executed service, casted to the specified type
     */
    public <T> T getExecutionResult(){
        //noinspection unchecked
        return (T)executionResult;
    }

    /**
     * Gets the request proceed by the executed service
     * @return the request proceed by the executed service
     */
    public IServiceRequestInfo getProceedRequest() {
        return proceedRequest;
    }
}
