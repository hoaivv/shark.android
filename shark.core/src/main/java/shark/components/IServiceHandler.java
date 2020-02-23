package shark.components;

public interface IServiceHandler {

    int computeIdentifier(IServiceRequestInfo request) throws ServiceException;

    int determineResponseCachingTime(IServiceRequestInfo request, Object response);

    Object process(IServiceRequestInfo request) throws ServiceException;

    Class<?> getDataClass();
    Class<?> getReturnClass();
}