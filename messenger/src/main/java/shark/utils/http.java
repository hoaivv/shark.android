package shark.utils;

import com.google.gson.Gson;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;


import shark.runtime.Function;
import shark.runtime.Promise;

public final class http {

    private http(){}

    public static http Client = new http();

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

        public <T> Promise<T> getObject(T instance) {

            final Promise<T> result = new Promise<>();

            if (connection == null || instance == null) {
                result.resolve(null);
                return result;
            }

            final Type type = instance.getClass();

            new Thread(new Runnable() {
                @Override
                public void run() {

                    InputStreamReader reader = null;

                    try {
                        reader = new InputStreamReader(connection.getInputStream());

                        result.resolve((T)new Gson().fromJson(reader, type));
                    }
                    catch (Exception e1) {

                        try{
                            if (reader != null) reader.close();
                        }
                        catch (Exception e2) {
                        }

                        result.resolve(null);
                    }
                    finally {
                        close();
                    }
                }
            }).start();

            return result;
        }

        public Promise<byte[]> getBytes() {

            final Promise<byte[]> result = new Promise<>();

            if (connection == null) {
                result.resolve(null);
                return  result;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {

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
                    }
                    catch (Exception e1){

                        try{
                            if (stream != null) stream.close();
                        }
                        catch (Exception e2){
                        }

                        result.resolve(null);
                    }
                    finally {
                        close();
                    }
                }
            }).start();

            return result;
        }

        public Promise<String> getString() {

            return getBytes().<String>then(new Function<byte[], String>() {
                @Override
                public String process(byte[] data) {
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

    public Promise<Response> get(String url) {

        final Promise<Response> result = new Promise<>();
        final URL target;

        try {
             target = new URL(url);
        }
        catch (MalformedURLException e){
            result.resolve(null);
            return result;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                Response response = null;

                try
                {
                    response = new Response((HttpURLConnection)target.openConnection());
                }
                catch (Exception e) {
                }
                finally {
                    result.resolve(response);
                }
            }
        }).start();

        return result;
    }

    public Promise<Response> post(String url, final String contentType, final byte[] data) {

        final Promise<Response> result = new Promise<>();
        final URL target;

        try {
            target = new URL(url);
        }
        catch (MalformedURLException e){
            result.resolve(null);
            return result;
        }

        if (contentType == null || contentType.length() == 0 || data == null) {
            result.resolve(null);
            return result;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                HttpURLConnection connection = null;
                OutputStream stream = null;
                Response response = null;
                try
                {
                    connection = (HttpURLConnection)target.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type",contentType);

                    if (data.length > 0) {
                        connection.setDoOutput(true);
                        stream = connection.getOutputStream();

                        stream.write(data);
                        stream.flush();
                        stream.close();
                        stream = null;
                    }

                    response = new Response(connection);
                }
                catch (Exception ex) {

                    try {
                        if (stream != null) stream.close();
                        if (connection != null) connection.disconnect();
                    }
                    catch (IOException io){
                    }
                }
                finally {
                    result.resolve(response);
                }

            }
        }).start();

        return result;
    }

    public Promise<Response> post(String url, Object obj) {

        try {
            return post(url, "application/json", new Gson().toJson(obj).getBytes("UTF-8"));
        }
        // this exception could not be thrown
        catch (UnsupportedEncodingException ex){
            return null;
        }
    }

    public Promise<Response> post(String url, String contentType, String data) {

        if (data == null) {
            Promise<Response> result = new Promise<>();
            result.resolve(null);
            return result;
        }

        try {
            return post(url, contentType, data.getBytes("UTF-8"));
        }// this exception could not be thrown
        catch (UnsupportedEncodingException ex){
            return null;
        }
    }
}
