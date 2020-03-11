package shark.runtime;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import shark.Framework;
import shark.io.File;
import shark.utils.Log;

@SuppressWarnings("WeakerAccess")
public class StoredStates {

    private static final HashMap<String, byte[]> _states = new HashMap<>();
    private static boolean isLoaded = false;

    private StoredStates() {

        File location;

        try {
            location = getLocation();
        }
        catch (InterruptedException e) {

            isLoaded = false;
            return;
        }

        if (location.exists() && location.isFile()) {

            BufferedReader reader = null;

            try {

                String[] stored = location.readAllLines();
                _states.clear();

                for (String state : stored) {
                    String[] parts = state.split(":");
                    if (parts.length == 2 && parts[0] != null && parts[0].length() > 0) {
                        _states.put(new String(Base64.decode(parts[0], Base64.DEFAULT)), Base64.decode(parts[1], Base64.DEFAULT));
                    }
                }

                isLoaded = true;
                if (Framework.debug) Log.information(StoredStates.class,
                        "States are loaded");
            } catch (Exception e) {
                isLoaded = false;
                Log.error(StoredStates.class,
                        "Failed to load states");
            } finally {

                //noinspection ConstantConditions
                if (reader != null) try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        } else {
            isLoaded = true;
        }

    }

    private static final StoredStates signature = new StoredStates();

    public static boolean isLoaded() {
        return isLoaded;
    }

    public static File getLocation() throws InterruptedException {

        return new File( Framework.getDataDirectory() + "/states");
    }

    private static boolean _update() {

        if (!isLoaded) return false;

        synchronized (signature) {
            StringBuilder data = new StringBuilder();

            for (String key : _states.keySet()) {
                data.append(Base64.encodeToString(key.getBytes(), Base64.NO_WRAP)).append(":").append(Base64.encodeToString(_states.get(key), Base64.NO_WRAP)).append("\n");
            }

            try {
                getLocation().writeAllText(data.substring(0, data.length() - 1));
                if (Framework.debug) Log.information(StoredStates.class, "States are saved");
                return true;
            } catch (Exception e) {
                Log.error(StoredStates.class, "States are not saved");
                return false;
            }
        }
    }

    public static boolean delete(Class<?> owner, String name) {

        if (!isLoaded) return false;

        if (owner == null || name == null || name.length() == 0)
            throw new IllegalArgumentException();

        String key = owner.getName() + ":" + name;

        synchronized (signature) {

            if (!_states.containsKey(key)) return true;

            byte[] old = _states.remove(key);

            if (_update()) {
                return true;
            } else {
                _states.put(key, old);
                return false;
            }

        }
    }

    public static byte[] get(Class<?> owner, String name) {

        if (owner == null || name == null || name.length() == 0)
            throw new IllegalArgumentException();

        String key = owner.getName() + ":" + name;

        synchronized (signature) {
            return _states.containsKey(key) ? _states.get(key) : null;
        }
    }

    public static String getString(Class<?> owner, String name, String onFailed) {
        byte[] data = get(owner, name);
        return data == null ? onFailed : new String(data);
    }

    public static boolean getBoolean(Class<?> owner, String name, boolean onFailed) {
        byte[] data = get(owner, name);

        return data == null || data.length != 1 ? onFailed : data[0] == 1;
    }

    public static byte getByte(Class<?> owner, String name, byte onFailed) {
        byte[] data = get(owner, name);
        return data == null || data.length != 1 ? onFailed : data[0];
    }

    public static int getInt(Class<?> owner, String name, int onFailed) {

        byte[] data = get(owner, name);
        return data == null ? onFailed : ByteBuffer.wrap(data).getInt();
    }

    public static long getLong(Class<?> owner, String name, long onFailed) {
        byte[] data = get(owner, name);
        return data == null ? onFailed : ByteBuffer.wrap(data).getLong();
    }

    public static float getFloat(Class<?> owner, String name, float onFailed) {
        byte[] data = get(owner, name);
        return data == null ? onFailed : ByteBuffer.wrap(data).getFloat();
    }

    public static double getDouble(Class<?> owner, String name, double onFailed) {
        byte[] data = get(owner, name);
        return data == null ? onFailed : ByteBuffer.wrap(data).getDouble();
    }

    public static boolean set(Class<?> owner, String name, final byte[] value) {

        if (owner == null || name == null || name.length() == 0)
            throw new IllegalArgumentException();

        final String key = owner.getName() + ":" + name;

        synchronized (signature) {

            byte[] old = _states.remove(key);
            _states.put(key, value);

            if (_update()) {
                return true;
            } else {
                if (old == null) {
                    _states.remove(key);
                } else {
                    _states.put(key, old);
                }

                return false;
            }
        }
    }

    public static boolean set(Class<?> owner, String name, String value) {

        if (value == null) return delete(owner, name);
        return set(owner, name, value.getBytes());
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean set(Class<?> owner, String name, boolean value) {
        return set(owner, name, new byte[] { value ? (byte)1 : 0 });
    }

    public static boolean set(Class<?> owner, String name, byte value) {
        return set(owner, name, new byte[] { value });
    }

    public static boolean set(Class<?> owner, String name, int value) {
        return set(owner, name, ByteBuffer.allocate(4).putInt(value).array());
    }

    public static boolean set(Class<?> owner, String name, long value) {
        return set(owner, name, ByteBuffer.allocate(8).putLong(value).array());
    }

    public static boolean set(Class<?> owner, String name, float value) {
        return set(owner, name, ByteBuffer.allocate(4).putFloat(value).array());
    }

    public static boolean set(Class<?> owner, String name, double value) {
        return set(owner, name, ByteBuffer.allocate(8).putDouble(value).array());
    }
}
