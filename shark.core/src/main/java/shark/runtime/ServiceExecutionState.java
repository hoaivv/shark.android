package shark.runtime;

import shark.components.IServiceRequestInfo;

/**
 * Provides information of an executing Shark Service
 */
public class ServiceExecutionState extends AsyncOperationState {

    IServiceRequestInfo request;

    /**
     * Gets the request, passed to the service to execute
     * @return
     */
    public IServiceRequestInfo getRequest(){
        return request;
    }

    ServiceExecutionState(IServiceRequestInfo request){
        super();
        this.request = request;
    }

    void _notifyFailure(Exception e) {
        notifyFailure(e);
    }
}
