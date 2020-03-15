package shark.net;

import shark.components.ServiceRequestInfo;

public class NetworkServiceRequestInfo<T> extends ServiceRequestInfo<T> implements INetworkServiceRequestInfo {

    private final Connection connection;

    @Override
    public Connection getConnection() {
        return connection;
    }

    private final Object requestState;

    @Override
    public Object getRequestState() {
        return requestState;
    }

    private final Object responseState;

    @Override
    public Object getResponseState() {
        return responseState;
    }

    public NetworkServiceRequestInfo(Connection connection, Object requestState, Object responseState, T data){
        super(data);
        this.connection = connection;
        this.requestState = requestState;
        this.responseState = responseState;
    }
}
