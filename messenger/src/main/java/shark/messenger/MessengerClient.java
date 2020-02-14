package shark.messenger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

import shark.runtime.Action;
import shark.runtime.Promise;

public class MessengerClient implements IPushableTarget {

    MessengerChannel _channel = null;
    String _server = null;
    HashSet<Action<MessengerData>> _onDataEventListeners = new HashSet<Action<MessengerData>>();
    ReentrantLock lock = new ReentrantLock();

    <T> MessengerResponse<T> _GET(Type typeOfT, String server, String path) {

        HttpURLConnection connection = null;
        InputStreamReader reader = null;
        try
        {
            connection = (HttpURLConnection)(new URL("http://" + server + "/messenger/" + path)).openConnection();
            reader = new InputStreamReader(connection.getInputStream());

            return new Gson().fromJson(reader, TypeToken.getParameterized(MessengerResponse.class, typeOfT).getType());
        }
        catch (Exception e) {

            return null;
        }
        finally {

            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException ioe){
                }
            }

            if (connection != null) connection.disconnect();
        }
    }
    <T> MessengerResponse<T> _POST(Type typeOfT, String server, String path, Object obj) {

        HttpURLConnection connection = null;
        InputStreamReader reader = null;
        OutputStreamWriter writer = null;
        try
        {
            connection = (HttpURLConnection)(new URL("http://" + server + "/messenger/" + path)).openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            connection.setDoOutput(true);

            Gson gson = new Gson();

            writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(gson.toJson(obj));
            writer.close();
            writer = null;

            reader = new InputStreamReader(connection.getInputStream());
            return gson.fromJson(reader, TypeToken.getParameterized(MessengerResponse.class, typeOfT).getType());
        }
        catch (Exception e) {

            return null;
        }
        finally {

            if (writer != null) {
                try {
                    writer.close();
                }
                catch (IOException ioe){
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException ioe){
                }
            }

            if (connection != null) connection.disconnect();
        }
    }

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

        this._server = server;
        this.interval = interval;
    }

    public MessengerClient(String server){

        this._server = server;
    }

    class Puller extends Thread {

        MessengerChannel channel;
        MessengerClient client;

        public void run() {

            try {
                while (client._channel == channel) {
                    MessengerResponse<MessengerData[]> response = _GET(MessengerData.class, client._server, channel.PullKey);

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
            catch (InterruptedException e) {
            }
        }

        public Puller(MessengerClient client, MessengerChannel channel){
            this.channel = channel;
            this.client = client;
        }
    }

    public Promise<Boolean> register(String description) throws IllegalArgumentException {
        if (description == null || description.length() == 0)
            throw new IllegalArgumentException("description");

        final MessengerChannel request = new MessengerChannel();

        request.Description = description;

        final MessengerClient client = this;
        final Promise<Boolean> result = new Promise<>();

        new Thread(new Runnable() {
            @Override
            public void run() {

                MessengerResponse<MessengerChannel> response = _POST(MessengerChannel.class, _server, "register", request);
                MessengerChannel channel = _channel = response == null || !response.Succeed ? null : response.Data;

                if (channel != null) {
                    new Puller(client, channel).run();
                }

                result.resolve(channel != null);
            }
        }).start();

        return result;
    }

    public void unregister(){
        _channel = null;
    }

    public Promise<Boolean> push(IPushableTarget target, String type, String data){

        MessengerChannel channel = _channel;

        final String sender = channel == null ? null : channel.PushKey;
        final String receiver = target == null ? null : target.getPushKey();

        final Promise<Boolean> result = new Promise<>();

        if (sender == null || sender.length() == 0 || receiver == null || receiver.length() == 0 || type == null || type.length() == 0){

            result.resolve(false);
            return result;
        }

        final MessengerData request = new MessengerData();

        request.PushKey = sender;
        request.Type = type;
        request.Data = data;

        new Thread(new Runnable() {
            @Override
            public void run() {
                MessengerResponse<Long> response = _POST(Long.class, _server, receiver, request);
                result.resolve(response != null && response.Succeed && response.Data > 0);
            }
        }).start();

        return result;
    }

    public Promise<MessengerChannel[]> getChannels() {

        final Promise<MessengerChannel[]> result = new Promise<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                MessengerResponse < MessengerChannel[]> response =  _GET(MessengerChannel[].class, _server, "channels");
                result.resolve(response != null && response.Succeed == true ? response.Data : new MessengerChannel[0]);
            }
        }).start();

        return result;
    }
}
