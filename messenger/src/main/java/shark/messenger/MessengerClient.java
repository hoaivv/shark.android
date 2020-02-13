package shark.messenger;

import android.support.v4.util.Consumer;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.HttpsURLConnection;

public class MessengerClient implements IPushableTarget {

    MessengerChannel _channel = null;
    String _server = null;
    HashSet<Consumer<MessengerData>> _onDataEventListeners = new HashSet<Consumer<MessengerData>>();
    ReentrantLock lock = new ReentrantLock();

    <T> MessengerResponse<T> _GET(String server, String path) {

        HttpURLConnection connection = null;
        InputStreamReader reader = null;
        try
        {
            connection = (HttpURLConnection)(new URL("http://" + server + "/messenger/" + path)).openConnection();
            connection.connect();

            reader = new InputStreamReader(connection.getInputStream());

            return new Gson().fromJson(reader, new MessengerResponse<T>(){}.getClass());
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
    <T> MessengerResponse<T> _POST(String server, String path, Object obj) {

        HttpURLConnection connection = null;
        InputStreamReader reader = null;
        OutputStreamWriter writer = null;
        try
        {
            connection = (HttpURLConnection)(new URL("http://" + server + "/messenger/" + path)).openConnection();

            connection.connect();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            connection.setDoOutput(true);

            Gson gson = new Gson();

            writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(gson.toJson(obj));
            writer.close();
            writer = null;

            reader = new InputStreamReader(connection.getInputStream());
            return gson.fromJson(reader, new MessengerResponse<T>(){}.getClass());
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

    public void addOnDataEventListener(Consumer<MessengerData> listener) {

        try {
            lock.lock();

            _onDataEventListeners.add(listener);
        }
        finally {
            lock.unlock();
        }
    }

    public void removeOnDataEventListener(Consumer<MessengerData> listener){

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
                    MessengerResponse<MessengerData[]> response = _GET(client._server, channel.PullKey);

                    MessengerData[] all = response == null || !response.Succeed || response.Data == null ? new MessengerData[0] : response.Data;

                    for (MessengerData one : all) {

                        try {
                            lock.lock();

                            for (Consumer<MessengerData> listener : client._onDataEventListeners)
                                listener.accept(one);
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

    public boolean register(String description) throws IllegalArgumentException
    {
        if (description == null || description.length() == 0) throw new IllegalArgumentException("description");

        MessengerChannel request = new MessengerChannel();
        request.Description = description;

        MessengerResponse<MessengerChannel> response = _POST(_server, "register", request);

        MessengerChannel channel = _channel = response == null || !response.Succeed ? null : response.Data;

        if (channel != null){

            new Puller(this, channel).run();
            return true;
        }

        return  false;
    }

    public void unregister(){
        _channel = null;
    }

    public boolean push(IPushableTarget target, String type, String data){

        MessengerChannel channel = _channel;

        String sender = channel == null ? null : channel.PushKey;
        String receiver = target == null ? null : target.getPushKey();

        if (sender == null || sender.length() == 0 || receiver == null || receiver.length() == 0 || type == null || type.length() == 0) return false;

        MessengerData request = new MessengerData();
        request.PushKey = sender;
        request.Type = type;
        request.Data = data;

        MessengerResponse<Long> response = _POST(_server, receiver, request);

        return  response != null && response.Succeed && response.Data > 0;
    }

    public MessengerChannel[] getChannels() {
        MessengerResponse<MessengerChannel[]> response =  _GET(_server, "channels");
        return response != null && response.Succeed == true ? response.Data : new MessengerChannel[0];
    }
}
