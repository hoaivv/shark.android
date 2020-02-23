package shark.components;

public class NotProceedServiceException extends ServiceException {

    public NotProceedServiceException(){
        super();
    }

    public NotProceedServiceException(String message){
        super(message);
    }

    public  NotProceedServiceException(String message, Throwable cause){
        super(message, cause);
    }
}
