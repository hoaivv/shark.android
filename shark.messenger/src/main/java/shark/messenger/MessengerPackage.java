package shark.messenger;

import java.util.Date;

@SuppressWarnings("WeakerAccess")
public class MessengerPackage implements MessengerTarget {

    public String PushKey;
    public String Type;
    public String Data;
    public long PushStamp;

    public String getPushKey() {
        return PushKey;
    }

    public Date getPushTime() {
        return  new Date(PushStamp);
    }
}
