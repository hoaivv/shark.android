package shark.utils;

import android.os.Looper;

import com.google.gson.Gson;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import shark.delegates.Action1;
import shark.runtime.Promise;

/**
 * Provides fast access to resources, served via HTTP/HTTPS protocol. This class does not allow its
 * instances to be created. Use static methods of this class to make requests.
 *
 * @see #at(String)
 * @see #get(String)
 * @see #get(String, Class)
 * @see #post(String, String, byte[])
 * @see #post(String, String, byte[], Class)
 * @see #post(String, String, String)
 * @see #post(String, String, String, Class)
 * @see #post(String, Object)
 * @see #post(String, Object, Class)
 */
public final class http {

    private http(){}

    private static http singleton = new http();

    /**
     * If this property is {@code true}, {@code https://} will be added to target URL when no
     * protocol is specified in it.
     * If this property is {@code false}, {@code http://} will be be
     * added to target URL when no protocol is specified in it.
     * By default, this value is {@code false}
     */
    public static boolean useHttpsByDefault = false;

    /**
     * Describes a HTTP request. This class does not allow its instance to be created directly. Use
     * {@link #at(String)} to generate instance of this class. Methods of this class allow request
     * customization.
     *
     * @see #at(String)
     * @see #header(String, String)
     * @see #body(byte[])
     * @see #body(String, byte[])
     * @see #body(Object)
     * @see #send()
     * @see #expect(Class)
     */
    public final class Request {

        private HashMap<String, String> headers = new HashMap<>();
        private String method;
        private String url;
        private byte[] body;

        /**
         * Creates a request
         * @param url Target of the request. If no protocol is specified in this value the protocol
         *            will be added according to value of {@link #useHttpsByDefault}
         */
        private Request(String url) {
            this.method = "GET";

            if (!url.startsWith("http://") && !url.startsWith("https://"))
                url = (useHttpsByDefault ? "https://" : "http://") + url;

            this.url = url;
        }

        /**
         * Sets request method. If request method is not defined by calling this method, by default
         * request method is set to GET.
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
         * type is {@link Response} the HTTP response will be returned otherwise response body will
         * be treated as a json string and the object of expecting type will be extracted from it.
         * @param expect expecting type of response.
         * @return instance of the expecting type via {@link Promise} if succeed; otherwise null via
         * {@link Promise}.
         */
        public <T> Promise<T> expect(Class<T> expect) {

            return send().then(response -> {

                try {
                    return expect == Response.class ? (T) response : response != null && response.getStatus() == 200 ? response.getObject(expect).result() : null;
                }
                catch (Exception e) {
                    return null;
                }
            });
        }

        /**
         * Sends current HTTP request
         * @return instance of {@link Response} via {@link Promise} if succeed; otherwise null via
         * {@link Promise}
         */
        public Promise<Response> send() {

            Promise<Response> promise = new Promise<>();
            Action1<Response> resolver = Promise.getResolver(promise);

            Runnable commit = () -> {

                URL target;

                try {
                    target = new URL(url);
                } catch (MalformedURLException e) {
                    resolver.run(null);
                    return;
                }

                HttpURLConnection connection = null;
                OutputStream stream = null;
                Response response = null;

                try {
                    connection = (HttpURLConnection) target.openConnection();

                    connection.setRequestMethod(method);

                    for (String key : headers.keySet()) {
                        connection.setRequestProperty(key, headers.get(key));
                    }

                    if (body != null && body.length > 0) {

                        connection.setDoOutput(true);
                        stream = connection.getOutputStream();

                        stream.write(body);
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

                resolver.run(response);
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
    }

    /**
     * Describes a HTTP response. Instances of this class will be generated when a HTTP/HTTPS
     * requests are made and their responses become available.
     */
    public final class Response implements Closeable {

        HttpURLConnection connection = null;

        private Response(HttpURLConnection connection) throws IllegalArgumentException {

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

            Promise<T> promise = new Promise<>();
            Action1<T> resolver = Promise.getResolver(promise);

            if (connection == null || type == null) {
                resolver.run(null);
                return promise;
            }

            Runnable commit = () -> {

                InputStreamReader reader = null;

                try {
                    reader = new InputStreamReader(connection.getInputStream());

                    resolver.run((T) new Gson().fromJson(reader, type));
                } catch (Exception e1) {

                    try {
                        if (reader != null) reader.close();
                    } catch (Exception e2) {
                    }

                    resolver.run(null);
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

            return promise;
        }

        /**
         * Gets response body as a byte array
         * @return response body as a byte array if succeed; otherwise null
         */
        public Promise<byte[]> getBytes() {

            Promise<byte[]> promise = new Promise<>();
            Action1<byte[]> resolver = Promise.getResolver(promise);

            if (connection == null) {
                resolver.run(null);
                return  promise;
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

                    resolver.run(data);
                } catch (Exception e1) {

                    try {
                        if (stream != null) stream.close();
                    } catch (Exception e2) {
                    }

                    resolver.run(null);
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

            return promise;
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

    /**
     * Makes a HTTP request to a specified URL using GET method
     * @param url URL to be requested
     * @return HTTP response via {@link Promise}
     */
    public static Promise<Response> get(String url) {
        return at(url).method("GET").send();
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
        return at(url).method("GET").expect(expect);
    }

    /**
     * Makes a HTTP request to a specified URL using POST method
     * @param url URL to be requested
     * @param contentType content type of request body
     * @param data request body
     * @return HTTP response via {@link Promise}
     */
    public static Promise<Response> post(String url, String contentType, byte[] data) {
        return at(url).method("POST").body(contentType, data).send();
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
        return at(url).method("POST").body(contentType, data).expect(type);
    }

    /**
     * Makes a HTTP request to a specified URL using POST method. This method will send a request
     * with content type set to application/json
     * @param url URL to be requested
     * @param obj Object to be sent in the request body as a json string
     * @return HTTP response via {@link Promise}
     */
    public static Promise<Response> post(String url, Object obj) {
        return at(url).method("POST").body(obj).send();
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
        return at(url).method("POST").body(obj).expect(expect);
    }

    /**
     * Makes a HTTP request to a specified URL using POST method
     * @param url URL to be requested
     * @param contentType content type of request body
     * @param data request body
     * @return HTTP response via {@link Promise}
     */
    public static Promise<Response> post(String url, String contentType, String data) {
        return at(url).method("POST").body(contentType, data.getBytes()).send();
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
        return at(url).method("POST").body(contentType, data.getBytes()).expect(expect);
    }

    /**
     * Creates a HTTP request
     * @param url Target of the request. If no protocol is specified in this value the protocol will
     *            be added according to value of {@link #useHttpsByDefault}
     * @return an instance of {@link Request}
     */
    public static Request at(String url) {
        return singleton.new Request(url);
    }
}
