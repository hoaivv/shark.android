package shark.components;

/**
 * Describes a handler of a Shark Service
 */
public interface IServiceHandler extends ISharkComponent {

    /**
     * Computes an identifier which could be used to identify identical requests.
     * {@link shark.Framework} use these identifiers to economize the number of time a service to be
     * invoked by grouping identical requests.
     * @param request request, identifier of which to be computed
     * @return identifier of the request
     * @throws ServiceException throws if the request is not sufficient for the operation
     */
    int computeIdentifier(IServiceRequestInfo request) throws ServiceException;

    /**
     * Determines how long the result associated with a specified request should be cached by
     * {@link shark.Framework}. Requests identical to the specified request will be served with
     * cached result if the cached result exists.
     * @param request associated request
     * @param response service processing result
     * @return number of milliseconds the result to be cached.
     */
    int determineResponseCachingTime(IServiceRequestInfo request, Object response);

    /**
     * Process a request
     * @param request request to be proceed
     * @return service processing result
     * @throws ServiceException throws if error occurred while service is processing the request
     */
    Object process(IServiceRequestInfo request) throws ServiceException;

    /**
     * Gets class of expected request data that the service could process
     */
    Class<?> getDataClass();

    /**
     * Gets class of expected response data that the service should return
     */
    Class<?> getReturnClass();
}
