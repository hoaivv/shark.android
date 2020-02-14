package shark.messenger;

public class MessengerChannel implements MessengerTarget {

    public String PushKey;
    public String PullKey;
    public String Description;
    public long Stamp;

    public String getPushKey(){
        return PushKey;
    }
}
