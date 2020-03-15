package shark.net;

import shark.components.IServiceRequestInfo;

interface INetworkServiceRequestInfo extends IServiceRequestInfo {
    Object getRequestState();
    Object getResponseState();
    Connection getConnection();
}
