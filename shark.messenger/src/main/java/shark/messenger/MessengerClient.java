package shark.messenger;

import java.util.concurrent.locks.ReentrantLock;

import shark.runtime.Action;
import shark.runtime.Function;
import shark.runtime.Promise;
import shark.utils.http;

public class MessengerClient implements MessengerTarget {

    MessengerChannel _channel = null;
    String _server = null;

    public Action<MessengerPackage> onPackageReceived = null;
    public Runnable onChannelRegistered = null;
    public Runnable onChannelTerminated = null;

    ReentrantLock lock = new ReentrantLock();

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

                    http.Response httpResponse = http.Client.get(_server + "/" + channel.PullKey).getResult();
                    PullResponse response = httpResponse == null ? null : httpResponse.getObject(new PullResponse()).getResult();

                    if (response != null && response.Succeed && response.Data != null) {

                        Action<MessengerPackage> callback = onPackageReceived;
                        if (callback != null) for (MessengerPackage one : response.Data) try { callback.process(one); } catch (Exception e) { }

                        Thread.sleep(Math.max(1, client.interval));
                    }
                    else {

                        boolean alive = false;

                        for (MessengerChannel one : getChannels().getResult()) {
                            if (one.PushKey == channel.PushKey) {
                                alive = true;
                                break;
                            }
                        }

                        if (!alive) {

                            try {

                                lock.lock();

                                if (client._channel == channel) client.unregister();
                            }
                            finally {
                                lock.unlock();
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

        final MessengerChannel request = new MessengerChannel();
        request.Description = description;

        final MessengerClient client = this;

        return http.Client.post(_server + "/register", request).<Boolean>then(new Function<http.Response, Boolean>() {
            @Override
            public Boolean process(http.Response data) {

                RegisterResponse response = data == null ? null : data.getObject(new RegisterResponse()).getResult();
                MessengerChannel channel = response == null || !response.Succeed ? null : response.Data;

                if (channel != null) {

                    try {
                        lock.lock();

                        _channel = channel;

                        Runnable callback = onChannelRegistered;
                        if (callback != null) callback.run();
                    }
                    finally {

                        lock.unlock();
                        new Puller(client, channel).start();
                    }
                }

                return channel != null;
            }
        });
    }

    public void unregister(){

        try {

            lock.lock();

            if (_channel != null) {

                _channel = null;
                Runnable callback = onChannelTerminated;
                if (callback != null) callback.run();
            }
        }
        finally {
            lock.unlock();
        }
    }

    class PushResponse extends MessengerResponse{
        public Long Data;
    }

    public Promise<Boolean> push(MessengerTarget target, String type, String data){

        MessengerChannel channel = _channel;

        final String sender = channel == null ? null : channel.PushKey;
        final String receiver = target == null ? null : target.getPushKey();

        if (sender == null || sender.length() == 0 || receiver == null || receiver.length() == 0 || type == null || type.length() == 0) return new Promise<>(false);

        final MessengerPackage request = new MessengerPackage();

        request.PushKey = sender;
        request.Type = type;
        request.Data = data;

        return http.Client.post(_server + "/" + receiver, request).then(new Function<http.Response, Boolean>() {
            @Override
            public Boolean process(http.Response httpResponse) {

                PushResponse response = httpResponse == null ? null : httpResponse.getObject(new PushResponse()).getResult();
                return response != null && response.Succeed && response.Data > 0;
            }
        });
    }

    public Promise<Boolean> publish(String name, String data) {

        MessengerChannel channel = _channel;

        final String sender = channel == null ? null : channel.PullKey;
        final String receiver = channel == null ? null : channel.PushKey;

        if (sender == null || sender.length() == 0 || receiver == null || receiver.length() == 0 || name == null || name.length() == 0 || data == null) return new Promise<>(false);

        final MessengerPackage request = new MessengerPackage();

        request.PushKey = sender;
        request.Type = name;
        request.Data = data;

        return http.Client.post(_server + "/" + receiver, request).then(new Function<http.Response, Boolean>() {
            @Override
            public Boolean process(http.Response httpResponse) {

                PushResponse response = httpResponse == null ? null : httpResponse.getObject(new PushResponse()).getResult();
                return response != null && response.Succeed && response.Data > 0;
            }
        });
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

        return http.Client.post(_server + "/" + receiver, request).then(new Function<http.Response, Boolean>() {
            @Override
            public Boolean process(http.Response httpResponse) {

                PushResponse response = httpResponse == null ? null : httpResponse.getObject(new PushResponse()).getResult();
                return response != null && response.Succeed && response.Data > 0;
            }
        });
    }

    class ChannelsResponse extends MessengerResponse {
        public MessengerChannel[] Data;
    }

    public Promise<MessengerChannel[]> getChannels() {

        return http.Client.get(_server + "/channels").then(new Function<http.Response, MessengerChannel[]>() {
            @Override
            public MessengerChannel[] process(http.Response httpResponse) {
                ChannelsResponse response =  httpResponse == null ? null : httpResponse.getObject(new ChannelsResponse()).getResult();
                return response != null && response.Succeed == true ? response.Data : new MessengerChannel[0];
            }
        });
    }

    class ResourceNamesResponse extends MessengerResponse {
        public String[] Data;
    }

    public Promise<String[]> getResourceNames(MessengerChannel channel) {

        String key = channel == null ? null : channel.PushKey;

        if (key == null || key.length() == 0) return new Promise<>(new String[0]);

        channel = new MessengerChannel();
        channel.PushKey = key;

        return http.Client.post(_server + "/resources", channel).<String[]>then(new Function<http.Response, String[]>() {
            @Override
            public String[] process(http.Response httpResponse) {
                ResourceNamesResponse response = httpResponse == null ? null : httpResponse.getObject(new ResourceNamesResponse()).getResult();
                return response != null && response.Succeed == true && response.Data != null ? response.Data : new String[0];
            }
        });
    }

    public Promise<MessengerPackage> getResource(MessengerChannel channel, String name){

        String key = channel == null ? null : channel.PushKey;

        if (key == null || key.length() == 0 || name == null || name.length() == 0) return new Promise<MessengerPackage>(null);

        channel = new MessengerChannel();
        channel.PushKey = key;
        channel.PullKey = name;

        return http.Client.post(_server + "/resources", channel).<MessengerPackage>then(new Function<http.Response, MessengerPackage>() {
            @Override
            public MessengerPackage process(http.Response httpResponse) {
                PullResponse response = httpResponse == null ? null : httpResponse.getObject(new PullResponse()).getResult();
                return response != null && response.Succeed == true && response.Data != null && response.Data.length > 0 ? response.Data[0] : null;
            }
        });
    }
}
