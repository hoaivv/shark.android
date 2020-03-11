package shark.components;

public class ServiceRequestInfo<T> implements IServiceRequestInfo {

    private final T data;

    public T getData(){
        return data;
    }

    public ServiceRequestInfo(T data){
        this.data = data;
    }
}
