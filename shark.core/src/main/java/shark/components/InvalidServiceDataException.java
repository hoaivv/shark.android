package shark.components;

public class InvalidServiceDataException extends ServiceException {

    private ServiceDataTypes type;

    public ServiceDataTypes getDataType(){
        return type;
    }

    public InvalidServiceDataException(ServiceDataTypes type){
        super();
        this.type = type;
    }

    public InvalidServiceDataException(ServiceDataTypes type, String message){
        super(message);
        this.type = type;
    }

    public InvalidServiceDataException(ServiceDataTypes type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }
}
