package shark.components;

public abstract class ServiceHandler<TData, TReturn> implements IServiceHandler {

    public abstract Class<TData> getDataClass();
    public abstract Class<TReturn> getReturnClass();

    protected abstract TReturn process(ServiceRequestInfo<TData> request);

    protected int computeIdentifier(ServiceRequestInfo<TData> request){
        return request.getData() == null ? 0 : request.getData().hashCode();
    }

    public Object process(IServiceRequestInfo request) throws ServiceException {

        if (request == null) throw new InvalidServiceDataException(ServiceDataTypes.requestData, "Invalid request");

        ServiceRequestInfo<TData> converted;

        try {
             converted = (ServiceRequestInfo<TData>)request;
        }
        catch (ClassCastException e) {
            throw new InvalidServiceDataException(ServiceDataTypes.requestData, "invalid request");
        }

        try {
            return process(converted);
        }
        catch (Exception e) {
            throw  ServiceException.class.isAssignableFrom(e.getClass()) ? (ServiceException)e : new ServiceException("Error detected while processing request", e);
        }
    }

    public int computeIdentifier(IServiceRequestInfo request) throws ServiceException {

        if (request == null) throw new InvalidServiceDataException(ServiceDataTypes.requestData, "Invalid request");

        ServiceRequestInfo<TData> converted;

        try {
            converted = (ServiceRequestInfo<TData>)request;
        }
        catch (ClassCastException e) {
            throw new InvalidServiceDataException(ServiceDataTypes.requestData, "invalid request");
        }

        try {
            return computeIdentifier(converted);
        }
        catch (Exception e) {
            throw  ServiceException.class.isAssignableFrom(e.getClass()) ? (ServiceException)e : new ServiceException("Error detected while computing request identifier", e);
        }
    }

    public int determineResponseCachingTime(IServiceRequestInfo request, Object response) {

        if (request == null) return 0;

        ServiceRequestInfo<TData> convertedRequest;
        TReturn convertedResponse;

        try {
            convertedRequest = (ServiceRequestInfo<TData>)request;
            convertedResponse = (TReturn)response;
        }
        catch (ClassCastException e) {
            return 0;
        }

        try {
            return determineResponseCachingTime(convertedRequest, convertedResponse);
        }
        catch (Exception e) {
            return 0;
        }
    }

    protected int determineResponseCachingTime(ServiceRequestInfo<TData> request, TReturn response){
        return 0;
    }
}
