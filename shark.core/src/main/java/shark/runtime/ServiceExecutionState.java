package shark.runtime;

import shark.components.IServiceRequestInfo;

public class ServiceExecutionState extends AsyncOperationState {

    IServiceRequestInfo request;

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
