package shark.messenger;

import shark.delegates.Action;
import shark.delegates.Action1;
import shark.runtime.Promise;
import shark.runtime.events.ActionEvent;
import shark.runtime.events.ActionTrigger;
import shark.utils.http;

public class MessengerClient implements MessengerTarget {

    MessengerChannel _channel = null;
    String _server = null;

    public ActionEvent<MessengerPackage> onPackageReceived = new ActionEvent<>();
    private Action1<MessengerPackage> onPackageReceivedInvoker = ActionEvent.getInvoker(onPackageReceived);

    public ActionTrigger onChannelRegistered = new ActionTrigger();
    private Action onChannelRegisteredInvoker = ActionTrigger.getInvoker(onChannelRegistered);

    public ActionTrigger onChannelTerminated = new ActionTrigger();
    private Action getOnChannelTerminatedInvoker = ActionTrigger.getInvoker(onChannelTerminated);

    public int interval = 500;

    public String getServer(){
        return _server;
    }

    public boolean isReady(){
        return _channel != null;
    }

    public String getPushKey(){
        return _channel == null ? null : _channel.PushKey;
    }

    public String getDescription() {
        return _channel == null ? null : _channel.Description;
    }

    public MessengerClient(String server, int interval){

        this._server = "http://" + server + "/messenger";
        this.interval = interval;
    }

    public MessengerClient(String server){

        this._server = "http://" + server + "/messenger";
    }

    class PullResponse extends MessengerResponse {
        public MessengerPackage[] Data;
    }

    class Puller extends Thread {

        MessengerChannel channel;
        MessengerClient client;

        public void run() {

            try {
                while (client._channel == channel) {

                    PullResponse response = http.get(_server + "/" + channel.PullKey, PullResponse.class).result();

                    if (response != null && response.Succeed && response.Data != null) {

                        if (onPackageReceived.hasHandler()) for (MessengerPackage one : response.Data) try { onPackageReceivedInvoker.run(one); } catch (Exception e) { }

                        Thread.sleep(Math.max(1, client.interval));
                    }
                    else {

                        boolean alive = false;

                        for (MessengerChannel one : getChannels().result()) {
                            if (one.PushKey == channel.PushKey) {
                                alive = true;
                                break;
                            }
                        }

                        if (!alive) {

                            synchronized (this) {
                                if (client._channel == channel) client.unregister();
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
            }
        }

        public Puller(MessengerClient client, MessengerChannel channel){
            this.channel = channel;
            this.client = client;
        }
    }

    class RegisterResponse extends MessengerResponse {
        public MessengerChannel Data;
    }

    public Promise<Boolean> register(String description) {

        if (description == null || description.length() == 0) return new Promise<>(false);

        MessengerChannel request = new MessengerChannel();
        request.Description = description;

        return http
                .post(_server + "/register", request, RegisterResponse.class)
                .then(response -> {

                    MessengerChannel channel = response == null || !response.Succeed ? null : response.Data;

                    if (channel != null) {

                        synchronized (this) {
                            _channel = channel;
                            try { onChannelRegisteredInvoker.run(); } catch (Exception e) { }
                        }

                        new Puller(this, channel).start();
                    }

                    return channel != null;
                });
    }

    public void unregister(){

        synchronized (this) {
            if (_channel != null) {

                _channel = null;
                try { getOnChannelTerminatedInvoker.run(); } catch (Exception e) { }
            }
        }
    }

    class PushResponse extends MessengerResponse{
        public Long Data;
    }

    public Promise<Boolean> push(MessengerTarget target, String type, String data){

        MessengerChannel channel = _channel;

        String sender = channel == null ? null : channel.PushKey;
        String receiver = target == null ? null : target.getPushKey();

        if (sender == null || sender.length() == 0 || receiver == null || receiver.length() == 0 || type == null || type.length() == 0) return new Promise<>(false);

        MessengerPackage request = new MessengerPackage();

        request.PushKey = sender;
        request.Type = type;
        request.Data = data;

        return http
                .post(_server + "/" + receiver, request, PushResponse.class)
                .then(response -> response != null && response.Succeed && response.Data > 0);
    }

    public Promise<Boolean> publish(String name, String data) {

        MessengerChannel channel = _channel;

        String sender = channel == null ? null : channel.PullKey;
        String receiver = channel == null ? null : channel.PushKey;

        if (sender == null || sender.length() == 0 || receiver == null || receiver.length() == 0 || name == null || name.length() == 0 || data == null) return new Promise<>(false);

        MessengerPackage request = new MessengerPackage();

        request.PushKey = sender;
        request.Type = name;
        request.Data = data;

        return http
                .post(_server + "/" + receiver, request, PushResponse.class)
                .then(response -> response != null && response.Succeed && response.Data > 0);
    }

    public Promise<Boolean> delete(String name) {

        MessengerChannel channel = _channel;

        final String sender = channel == null ? null : channel.PullKey;
        final String receiver = channel == null ? null : channel.PushKey;

        if (sender == null || sender.length() == 0 || receiver == null || receiver.length() == 0 || name == null || name.length() == 0) return new Promise<>(false);

        final MessengerPackage request = new MessengerPackage();

        request.PushKey = sender;
        request.Type = name;
        request.Data = null;

        return http
                .post(_server + "/" + receiver, request, PushResponse.class)
                .then(response -> response != null && response.Succeed && response.Data > 0);
    }

    class ChannelsResponse extends MessengerResponse {
        public MessengerChannel[] Data;
    }

    public Promise<MessengerChannel[]> getChannels() {

        return http
                .get(_server + "/channels", ChannelsResponse.class)
                .then(response -> response != null && response.Succeed == true ? response.Data : new MessengerChannel[0]);
    }

    class ResourceNamesResponse extends MessengerResponse {
        public String[] Data;
    }

    public Promise<String[]> getResourceNames(MessengerChannel channel) {

        String key = channel == null ? null : channel.PushKey;

        if (key == null || key.length() == 0) return new Promise<>(new String[0]);

        channel = new MessengerChannel();
        channel.PushKey = key;



        return http
                .post(_server + "/resources", channel, ResourceNamesResponse.class)
                .then(response -> response != null && response.Succeed == true && response.Data != null ? response.Data : new String[0]);
    }

    public Promise<MessengerPackage> getResource(MessengerChannel channel, String name){

        String key = channel == null ? null : channel.PushKey;

        if (key == null || key.length() == 0 || name == null || name.length() == 0) return new Promise<MessengerPackage>(null);

        channel = new MessengerChannel();
        channel.PushKey = key;
        channel.PullKey = name;

        return http
                .post(_server + "/resources", channel, PullResponse.class)
                .then(response -> response != null && response.Succeed == true && response.Data != null && response.Data.length > 0 ? response.Data[0] : null);
    }
}
