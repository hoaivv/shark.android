package shark.runtime;

import shark.components.IServiceRequestInfo;

public final class ServiceExecutionResult {

    Object executionResult;
    IServiceRequestInfo proceedRequest;

    public ServiceExecutionResult(Object executionResult, IServiceRequestInfo proceedRequest){
        this.executionResult = executionResult;
        this.proceedRequest = proceedRequest;
    }

    public <T> T getExecutionResult(){
        return (T)executionResult;
    }

    public IServiceRequestInfo getProceedRequest() {
        return proceedRequest;
    }
}
