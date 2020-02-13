package shark.messenger;

public class MessengerChannel implements IPushableTarget {

    public String PushKey;
    public String PullKey;
    public String Description;
    public long Stamp;

    public String getPushKey(){
        return PushKey;
    }
}
