package shark.messenger;

import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

import shark.runtime.Action;
import shark.runtime.Function;
import shark.runtime.Promise;
import shark.utils.http;

public class MessengerClient implements IPushableTarget {

    MessengerChannel _channel = null;
    String _server = null;
    HashSet<Action<MessengerData>> _onDataEventListeners = new HashSet<Action<MessengerData>>();
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

    public void addOnDataEventListener(Action<MessengerData> listener) {

        try {
            lock.lock();

            _onDataEventListeners.add(listener);
        }
        finally {
            lock.unlock();
        }
    }

    public void removeOnDataEventListener(Action<MessengerData> listener){

        try {
            lock.lock();
            _onDataEventListeners.remove(listener);
        }
        finally {
            lock.unlock();
        }
    }

    public MessengerClient(String server, int interval){

        this._server = "http://" + server + "/messenger";
        this.interval = interval;
    }

    public MessengerClient(String server){

        this._server = "http://" + server + "/messenger";
    }

    class Puller extends Thread {

        MessengerChannel channel;
        MessengerClient client;

        public void run() {

            try {
                while (client._channel == channel) {

                    http.Response httpResponse = http.Client.get(_server + "/" + channel.PullKey).getResult();
                    MessengerResponse<MessengerData[]> response = httpResponse == null ? null : httpResponse.getObject(new MessengerResponse<MessengerData[]>()).getResult();
                    MessengerData[] all = response == null || !response.Succeed || response.Data == null ? new MessengerData[0] : response.Data;

                    for (MessengerData one : all) {

                        try {
                            lock.lock();

                            for (Action<MessengerData> listener : client._onDataEventListeners)
                                listener.process(one);
                        } finally {
                            lock.unlock();
                        }
                    }

                    Thread.sleep(Math.max(1, client.interval));
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

    public Promise<Boolean> register(String description) {

        if (description == null || description.length() == 0) return new Promise<>(false);

        final MessengerChannel request = new MessengerChannel();
        request.Description = description;

        final MessengerClient client = this;

        return http.Client.post(_server + "/register", request).<Boolean>then(new Function<http.Response, Boolean>() {
            @Override
            public Boolean process(http.Response data) {

                MessengerResponse<MessengerChannel> response = data == null ? null : data.getObject(new MessengerResponse<MessengerChannel>()).getResult();
                MessengerChannel channel = _channel = response == null || !response.Succeed ? null : response.Data;

                if (channel != null) {
                    new Puller(client, channel).run();
                }

                return channel != null;
            }
        });
    }

    public void unregister(){
        _channel = null;
    }

    public Promise<Boolean> push(IPushableTarget target, String type, String data){

        MessengerChannel channel = _channel;

        final String sender = channel == null ? null : channel.PushKey;
        final String receiver = target == null ? null : target.getPushKey();

        final Promise<Boolean> result = new Promise<>();

        if (sender == null || sender.length() == 0 || receiver == null || receiver.length() == 0 || type == null || type.length() == 0) return new Promise<>(false);

        final MessengerData request = new MessengerData();

        request.PushKey = sender;
        request.Type = type;
        request.Data = data;

        return http.Client.post(_server + "/" + receiver, request).then(new Function<http.Response, Boolean>() {
            @Override
            public Boolean process(http.Response httpResponse) {

                MessengerResponse<Long> response = httpResponse == null ? null : httpResponse.getObject(new MessengerResponse<Long>()).getResult();
                return response != null && response.Succeed && response.Data > 0;
            }
        });
    }

    public Promise<MessengerChannel[]> getChannels() {

        return http.Client.get(_server + "/channels").then(new Function<http.Response, MessengerChannel[]>() {
            @Override
            public MessengerChannel[] process(http.Response httpResponse) {
                MessengerResponse<MessengerChannel[]> response =  httpResponse == null ? null : httpResponse.getObject(new MessengerResponse<MessengerChannel[]>()).getResult();
                return response != null && response.Succeed == true ? response.Data : new MessengerChannel[0];
            }
        });
    }
}
