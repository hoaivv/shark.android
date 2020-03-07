package shark.utils;

import android.os.Looper;

import com.google.gson.Gson;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import shark.delegates.Function1;
import shark.runtime.Promise;

public final class http {

    private http(){}

    private static http singleton = new http();

    public final class Response implements Closeable {

        HttpURLConnection connection = null;

        public Response(HttpURLConnection connection) throws IllegalArgumentException {

            if (connection == null) throw new IllegalArgumentException();
            this.connection = connection;
        }

        public int getStatus() throws IOException {
            return connection.getResponseCode();
        }

        @Override
        public void close() {
            if (connection != null) connection.disconnect();
            connection = null;
        }

        public <T> Promise<T> getObject(Class<T> type) {

            final Promise<T> result = new Promise<>();

            if (connection == null || type == null) {
                result.resolve(null);
                return result;
            }

            Runnable commit = () -> {

                InputStreamReader reader = null;

                try {
                    reader = new InputStreamReader(connection.getInputStream());

                    result.resolve((T) new Gson().fromJson(reader, type));
                } catch (Exception e1) {

                    try {
                        if (reader != null) reader.close();
                    } catch (Exception e2) {
                    }

                    result.resolve(null);
                } finally {
                    close();
                }
            };

            if (Looper.myLooper() == Looper.getMainLooper()) {
                Thread thread = new Thread(commit);
                thread.setDaemon(true);
                thread.start();
            }
            else {
                commit.run();
            }

            return result;
        }

        public Promise<byte[]> getBytes() {

            final Promise<byte[]> result = new Promise<>();

            if (connection == null) {
                result.resolve(null);
                return  result;
            }

            Runnable commit = () -> {

                InputStream stream = null;

                try {

                    stream = connection.getInputStream();

                    int count = 0;
                    byte[] data = new byte[connection.getContentLength()];

                    while (count < data.length) {
                        count += stream.read(data, count, data.length - count);
                        if (count < data.length) Thread.sleep(10);
                    }

                    result.resolve(data);
                } catch (Exception e1) {

                    try {
                        if (stream != null) stream.close();
                    } catch (Exception e2) {
                    }

                    result.resolve(null);
                } finally {
                    close();
                }
            };

            if (Looper.myLooper() == Looper.getMainLooper()) {

                Thread thread = new Thread(commit);
                thread.setDaemon(true);
                thread.start();
            }
            else {

                commit.run();
            }

            return result;
        }

        public Promise<String> getString() {

            return getBytes().<String>then(new Function1<byte[], String>() {
                @Override
                public String run(byte[] data) {
                    try {
                        return new String(data, "UTF-8");
                    }
                    catch (Exception e){
                        return null;
                    }
                }
            });
        }
    }

    private Promise<Response> _get(final String url) {

        final Promise<Response> promise = new Promise<>();

        Runnable commit =  () -> {

            URL target;

            try {
                target = new URL(url);
            } catch (MalformedURLException e) {

                promise.resolve(null);
                return;
            }

            Response response = null;

            try {
                response = new http.Response((HttpURLConnection) target.openConnection());
            } catch (Exception e) {
            }

            promise.resolve(response);
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            Thread thread = new Thread(commit);
            thread.setDaemon(true);
            thread.start();
        }
        else {
            commit.run();
        }

        return promise;
    }

    private Promise<Response> _post(final String url, final String contentType, final byte[] data) {

        final Promise<Response> promise = new Promise<Response>();

        Runnable commit = () -> {

            URL target;

            try {
                target = new URL(url);
            } catch (MalformedURLException e) {
                promise.resolve(null);
                return;
            }

            if (contentType == null || contentType.length() == 0 || data == null) {
                promise.resolve(null);
                return;
            }

            HttpURLConnection connection = null;
            OutputStream stream = null;
            Response response = null;

            try {
                connection = (HttpURLConnection) target.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", contentType);

                if (data.length > 0) {
                    connection.setDoOutput(true);
                    stream = connection.getOutputStream();

                    stream.write(data);
                    stream.flush();
                    stream.close();
                    stream = null;
                }

                response = new http.Response(connection);
            } catch (Exception ex) {

                try {
                    if (stream != null) stream.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException io) {
                }
            }

            promise.resolve(response);
        };

        if (Looper.getMainLooper() == Looper.myLooper()) {
            Thread thread = new Thread(commit);
            thread.setDaemon(true);
            thread.start();
        }
        else {
            commit.run();
        }

        return promise;
    }

    public static Promise<Response> get(String url) {
        return singleton._get(url);
    }

    public static <T> Promise<T> get(String url, Class<T> type) {

        return get(url).then(response -> {
           try {
               return response != null && response.getStatus() == 200 ? response.getObject(type).getResult() : null;
           }
           catch (Exception e) {
               return null;
           }
        });
    }

    public static Promise<Response> post(String url, String contentType, byte[] data) {
        return singleton._post(url, contentType, data);
    }

    public static Promise<Response> post(String url, Object obj) {

        try {
            return post(url, "application/json", new Gson().toJson(obj).getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException ex){
            return new Promise<>(null);
        }
    }

    public static <T> Promise<T> post(String url, Object obj, Class<T> type) {
        return post(url, obj).then(response -> {
           try{
               return response != null && response.getStatus() == 200 ? response.getObject(type).getResult() : null;
           }
           catch (Exception e) {
               return null;
           }
        });
    }

    public static Promise<Response> post(String url, String contentType, String data) {

        if (data == null) return new Promise<Response>(null);
        try {
            return post(url, contentType, data.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException ex){
            return new Promise<>(null);
        }
    }

    public static <T> Promise<T> post(String url, String contentType, String data, Class<T> type) {

        return post(url, contentType, data).then(response -> {
            try {
                return response != null && response.getStatus() == 200 ? response.getObject(type).getResult() : null;
            }
            catch (Exception e) {
                return null;
            }
        });
    }
}
