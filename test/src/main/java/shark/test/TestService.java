package shark.test;

import shark.components.ServiceHandler;
import shark.components.ServiceRequestInfo;
import shark.components.SharkService;
import shark.components.SharkServiceAlternativeName;

@SharkService("test")
@SharkServiceAlternativeName({"a","b"})
public class TestService extends ServiceHandler<String, String> {

    @Override
    public Class<String> getDataClass() { return String.class; }

    @Override
    public Class<String> getReturnClass() { return String.class; }

    @Override
    protected String process(ServiceRequestInfo<String> request) {
        return "Hello " + request.getData();
    }
}
