package shark.components;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class ServiceHandler<TData, TReturn> implements IServiceHandler {

    private Class<TData> _D = null;
    private Class<TReturn> _R = null;

    protected ServiceHandler() {

        Class<?> cls = this.getClass();
        while (cls.getSuperclass() != ServiceHandler.class)  cls = cls.getSuperclass();
        Type[] types = ((ParameterizedType)cls.getGenericSuperclass()).getActualTypeArguments();

        _D = (Class<TData>)types[0];
        _R = (Class<TReturn>)types[1];
    }

    public final Class<TData> getDataClass() {
        return _D;
    }
    public final Class<TReturn> getReturnClass() {
        return _R;
    }

    protected abstract TReturn process(ServiceRequestInfo<TData> request);

    protected int computeIdentifier(ServiceRequestInfo<TData> request){
        return request.getData() == null ? 0 : request.getData().hashCode();
    }

    public Object process(IServiceRequestInfo request) throws ServiceException {

        if (request == null) throw new InvalidServiceDataException(ServiceDataTypes.RequestData, "Invalid request");

        ServiceRequestInfo<TData> converted;

        try {
             converted = (ServiceRequestInfo<TData>)request;
        }
        catch (ClassCastException e) {
            throw new InvalidServiceDataException(ServiceDataTypes.RequestData, "invalid request");
        }

        try {
            return process(converted);
        }
        catch (Exception e) {
            throw  ServiceException.class.isAssignableFrom(e.getClass()) ? (ServiceException)e : new ServiceException("Error detected while processing request", e);
        }
    }

    public int computeIdentifier(IServiceRequestInfo request) throws ServiceException {

        if (request == null) throw new InvalidServiceDataException(ServiceDataTypes.RequestData, "Invalid request");

        ServiceRequestInfo<TData> converted;

        try {
            converted = (ServiceRequestInfo<TData>)request;
        }
        catch (ClassCastException e) {
            throw new InvalidServiceDataException(ServiceDataTypes.RequestData, "invalid request");
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
