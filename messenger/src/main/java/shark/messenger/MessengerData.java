package shark.messenger;

import java.util.Date;

public class MessengerData implements IPushableTarget {

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
