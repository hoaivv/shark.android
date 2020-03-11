package shark.components;

/**
 * Describes exceptions related to data format, occurred when Shark Service is required to process
 */
@SuppressWarnings("WeakerAccess")
public class InvalidServiceDataException extends ServiceException {

    private final ServiceDataTypes type;

    /**
     * Type of data related to the exception
     * @return type of data related to the exception
     */
    public ServiceDataTypes getDataType(){
        return type;
    }

    /**
     * Generate an exception
     * @param type type of data related to the generating exception
     */
    public InvalidServiceDataException(ServiceDataTypes type){
        super();
        this.type = type;
    }

    /**
     * Generate an exception
     * @param type type of data related to the generating exception
     * @param message message to describe the cause
     */
    public InvalidServiceDataException(ServiceDataTypes type, String message){
        super(message);
        this.type = type;
    }

    /**
     * Generate an exception
     * @param type type of data related to the generating exception
     * @param message message to describe the cause
     * @param cause underlying exception that cause the exception to be generated
     */
    public InvalidServiceDataException(ServiceDataTypes type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }
}
