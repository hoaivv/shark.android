package shark.test;

import shark.components.ServiceHandler;
import shark.components.ServiceRequestInfo;
import shark.components.SharkService;
import shark.components.SharkServiceAlternativeName;

@SuppressWarnings("ALL")
@SharkService("test")
@SharkServiceAlternativeName({"a","b"})
public class TestService extends ServiceHandler<String, String> {

    @Override
    protected String process(ServiceRequestInfo<String> request) {
        return "Hello " + request.getData();
    }
}
