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
import java.util.HashMap;

import shark.runtime.Promise;

/**
 * Provides access to resources, served via HTTP protocol
 */
public final class http {

    private http(){}

    private static http singleton = new http();

    /**
     * Descrives a HTTP request
     */
    public final class Request {

        private HashMap<String, String> headers = new HashMap<>();
        private String method;
        private String url;
        private byte[] body;

        /**
         * Creates a request
         * @param url Target of the request
         */
        private Request(String url) {
            this.method = "GET";
            this.url = url;
        }

        /**
         * Sets request method
         * @param method HTTP method to be used, could be GET or HEAD or POST or PUT or DELETE
         *               or CONNECT or OPTIONS or TRACE or PATCH
         * @return current request object
         */
        public Request method(String method){
            this.method = method;
            return this;
        }

        /**
         * Sets a request header
         * @param name name of the header
         * @param value value of the header
         * @return current request object
         */
        public Request header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        /**
         * Sets request body
         * @param body body of the request
         * @return current request object
         */
        public Request body(byte[] body) {
            this.body = body;
            return this;
        }

        /**
         * Sets request body
         * @param mime MIME type of the body. Request header 'Content-Type' will be set to this
         *             value
         * @param body body of the request
         * @return current request object
         */
        public Request body(String mime, byte[] body) {

            headers.put("Content-Type", mime);
            this.body = body;
            return this;
        }

        /**
         * Sets request body. This method will set request header 'Content-Type' to
         * 'application/json' and set request body to json serialized string of the provided object.
         * The json serialized string will be encoding using UTF-8 charset.
         * @param body object to be set as request body
         * @return current request object
         */
        public Request body(Object body) {

            try {
                return body("application/json", new Gson().toJson(body).getBytes("UTF-8"));
            }
            catch (Exception ex)
            {
                return this;
            }
        }


        /**
         * Sends current HTTP request and expects response of a specified type. If the expecting
         * type is {@link Response} the HTTP response will be return otherwise response body will be
         * treated as a json string and the object of expecting type will be extracted from it.
         * @param expect expecting type of response.
         * @return instance of the expecting type via {@link Promise} if succeed; otherwise null via
         * {@link Promise}.
         */
        public <T> Promise<T> expect(Class<T> expect) {
            return expect == Response.class ? http.send(this).then(response -> (T)response) : http.send(this, expect);
        }
    }

    /**
     * Describes a HTTP response
     */
    public final class Response implements Closeable {

        HttpURLConnection connection = null;

        Response(HttpURLConnection connection) throws IllegalArgumentException {

            if (connection == null) throw new IllegalArgumentException();
            this.connection = connection;
        }

        /**
         * Get HTTP status code
         * @return HTTP status code of the response
         * @throws IOException throws if the response is not valid
         */
        public int getStatus() throws IOException {
            return connection.getResponseCode();
        }

        /**
         * Close the underlying streams and connections of the response
         */
        @Override
        public void close() {
            if (connection != null) connection.disconnect();
            connection = null;
        }

        /**
         * Converts response body from json string to an object
         * @param type class of object to be extracted
         * @param <T> type of object to be extracted
         * @return an instance of provided type if succeed; otherwise null
         */
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

        /**
         * Gets response body as a byte array
         * @return response body as a byte array if succeed; otherwise null
         */
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

        /**
         * Gets response body as a string
         * @return response body as a string if succeed; otherwise null
         */
        public Promise<String> getString() {

            return getBytes().then(data -> {
                try {
                    return new String(data, "UTF-8");
                }
                catch (Exception e) {
                    return null;
                }
            });
        }
    }

    private Promise<Response> _get(String url) {

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

    private Promise<Response> _post(String url, String contentType, byte[] data) {

        Promise<Response> promise = new Promise<Response>();

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

    private Promise<Response> _send(Request request) {

        Promise<Response> promise = new Promise<Response>();

        Runnable commit = () -> {

            URL target;

            try {
                target = new URL(request.url);
            } catch (MalformedURLException e) {
                promise.resolve(null);
                return;
            }

            HttpURLConnection connection = null;
            OutputStream stream = null;
            Response response = null;

            try {
                connection = (HttpURLConnection) target.openConnection();

                connection.setRequestMethod(request.method);

                for (String key : request.headers.keySet()) {
                    connection.setRequestProperty(key, request.headers.get(key));
                }

                if (request.body != null && request.body.length > 0) {

                    connection.setDoOutput(true);
                    stream = connection.getOutputStream();

                    stream.write(request.body);
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

    /**
     * Makes a HTTP request to a specified URL using GET method
     * @param url URL to be requested
     * @return HTTP response via {@link Promise}
     */
    public static Promise<Response> get(String url) {
        return singleton._get(url);
    }

    /**
     * Makes a HTTP request to a specified URL using GET method and converts response body from json
     * string to an object of a specified type
     * @param url URL to be requested
     * @param expect class of object to be extracted from response body
     * @param <T> type of object to be extracted from response body
     * @return instance of the specified type via {@link Promise} if succeed; otherwise null via
     * {@link Promise}
     */
    public static <T> Promise<T> get(String url, Class<T> expect) {

        return get(url).then(response -> {
           try {
               return response != null && response.getStatus() == 200 ? response.getObject(expect).getResult() : null;
           }
           catch (Exception e) {
               return null;
           }
        });
    }

    /**
     * Makes a HTTP request to a specified URL using POST method
     * @param url URL to be requested
     * @param contentType content type of request body
     * @param data request body
     * @return HTTP response via {@link Promise}
     */
    public static Promise<Response> post(String url, String contentType, byte[] data) {
        return singleton._post(url, contentType, data);
    }

    /**
     * Makes a HTTP request to a specified URL using POST method and converts response body from a
     * json string to an object of a specified type. This method will send a request with content
     * type set to application/json
     * @param url URL to be requested
     * @param contentType content type of request body
     * @param data request body
     * @param type class of the object to be extracted from response body
     * @param <T> type of the object to be extracted from response body
     * @return instance of the specified type via {@link Promise} if succeed; otherwise null via
     * {@link Promise}
     */
    public  static <T> Promise<T> post(String url, String contentType, byte[] data, Class<T> type) {
        return post(url, contentType, data).then(response -> {
            try {
                return response != null && response.getStatus() == 200 ? response.getObject(type).getResult() : null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Makes a HTTP request to a specified URL using POST method. This method will send a request
     * with content type set to application/json
     * @param url URL to be requested
     * @param obj Object to be sent in the request body as a json string
     * @return HTTP response via {@link Promise}
     */
    public static Promise<Response> post(String url, Object obj) {

        try {
            return post(url, "application/json", new Gson().toJson(obj).getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException ex){
            return new Promise<>(null);
        }
    }

    /**
     * Makes a HTTP request to a specified URL using POST method and converts response body from a
     * json string to an object of a specified type. This method will send a request with content
     * type set to application/json
     * @param url URL to be requested
     * @param obj Object to be sent in the request body as a json string
     * @param expect class of the object to be extracted from response body
     * @param <T> type of the object to be extracted from response body
     * @return instance of the specified type via {@link Promise} if succeed; otherwise null via
     * {@link Promise}
     */
    public static <T> Promise<T> post(String url, Object obj, Class<T> expect) {
        return post(url, obj).then(response -> {
           try{
               return response != null && response.getStatus() == 200 ? response.getObject(expect).getResult() : null;
           }
           catch (Exception e) {
               return null;
           }
        });
    }

    /**
     * Makes a HTTP request to a specified URL using POST method
     * @param url URL to be requested
     * @param contentType content type of request body
     * @param data request body
     * @return HTTP response via {@link Promise}
     */
    public static Promise<Response> post(String url, String contentType, String data) {

        if (data == null) return new Promise<Response>(null);
        try {
            return post(url, contentType, data.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException ex){
            return new Promise<>(null);
        }
    }

    /**
     * Makes a HTTP request to a specified URL using POST method and converts response body from a
     * json string to an object of a specified type. This method will send a request with content
     * type set to application/json
     * @param url URL to be requested
     * @param contentType content type of request body
     * @param data request body
     * @param expect class of the object to be extracted from response body
     * @param <T> type of the object to be extracted from response body
     * @return instance of the specified type via {@link Promise} if succeed; otherwise null via
     * {@link Promise}
     */
    public static <T> Promise<T> post(String url, String contentType, String data, Class<T> expect) {

        return post(url, contentType, data).then(response -> {
            try {
                return response != null && response.getStatus() == 200 ? response.getObject(expect).getResult() : null;
            }
            catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Creates a HTTP request
     * @param url Target of the request
     * @return an instance of {@link Request}
     */
    public static Request at(String url) {
        return singleton.new Request(url);
    }

    /**
     * Sends a HTTP request
     * @param request request to be sent
     * @return HTTP Response via {@link Promise}
     */
    public static Promise<Response> send(Request request) {
        return singleton._send(request);
    }

    /**
     * Sends a HTTP request and converts response body from a json string to an object of a
     * specified type.
     * @param request request to be sent
     * @param expect class of the object to be extracted from response body
     * @param <T> type of the object to be extracted from response body
     * @return instance of the specified type via {@link Promise} if succeed; otherwise null via
     * {@link Promise}
     */
    public static <T> Promise<T> send(Request request, Class<T> expect) {

        return send(request).then(response -> {
            try {
                return response != null && response.getStatus() == 200 ? response.getObject(expect).getResult() : null;
            }
            catch (Exception e) {
                return null;
            }
        });
    }
}
