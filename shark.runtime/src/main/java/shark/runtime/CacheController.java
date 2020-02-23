package shark.runtime;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;

import shark.Framework;
import shark.delegates.Function;
import shark.io.File;
import shark.runtime.events.ActionEvent;
import shark.runtime.events.FunctionTrigger;
import shark.runtime.serialization.JsonSerializer;
import shark.runtime.serialization.Serializer;
import shark.utils.Log;

final class CacheController {

    private class AllocationJson extends HashMap<String, Long> {
    }

    private static HashMap<String, Long> _caches = new HashMap<>();
    private static long _currentFileIndex = 0;
    private static Serializer _defaultSerializer = new JsonSerializer();

    private static Long lastCleanupStamp = null;

    static FunctionTrigger<Boolean> onCommitChangesToStorage = null;

    private static Function<Boolean> _commitChangesToStorage = new Function<Boolean>() {
        @Override
        public Boolean run() {
            synchronized (_caches) {
                try {

                    if (_caches.size() == _lastCacheCount) return true;

                    File file = new File(getCacheDirectory() + "/.allocation");

                    if (file.exists() && (!file.isFile() || !file.delete())) return false;

                    FileOutputStream output = null;

                    try {
                        output = new FileOutputStream(file);
                        _defaultSerializer.serialize(output, _caches);
                    }
                    finally {
                        if (output != null) output.close();
                    }

                    _lastCacheCount = _caches.size();
                    return true;
                }
                catch (Exception e) {
                    return false;
                }
            }
        }
    };

    private static Task _storingTask = new Task() {
        @Override
        public void run(Object state) {
            try {

                while (true) {

                    try {
                        setPersistent(onCommitChangesToStorage.invoke(signature, true));
                    }
                    catch (IllegalAccessException e) {
                    }

                    Thread.currentThread().join(1000);
                }
            }
            catch (InterruptedException e) {
            }
        }
    };

    private CacheController() {

        try {

            if (!isPersistent()) getCacheDirectory().delete(true);

        } catch (Exception e) {
        }

        try {

            if (!getCacheDirectory().exists() || !getCacheDirectory().isDirectory()) {

                if (!getCacheDirectory().mkdirs()) {
                    Log.error(CacheController.class, "Could not create cache directory");
                }
            }
        } catch (Exception e) {
            Log.error(CacheController.class, "Could not create cache directory");
        }

        synchronized (_caches) {

            try {

                File file = new File(getCacheDirectory() + "/.allocation");

                if (file.exists() && file.isFile()) {

                    FileInputStream input = null;

                    try {
                        input = new FileInputStream(file);

                        AllocationJson json = _defaultSerializer.deserialize(input, AllocationJson.class);

                        for (String key : json.keySet()) {
                            _caches.put(key, json.get(key));
                            _currentFileIndex = Math.max(_currentFileIndex, json.get(key));
                        }
                    } finally {
                        if (input != null) input.close();
                    }
                }
            } catch (Exception e) {
            }
        }

        Parallel.start(_storingTask, null, false);

        onCommitChangesToStorage = new FunctionTrigger<>(this);
        onCommitChangesToStorage.add(_commitChangesToStorage);
    }

    private static CacheController signature = new CacheController();

    static ActionEvent<Long> onCleanup = new ActionEvent<>(signature);

    private static int _lastCacheCount = 0;

    public static File getCacheDirectory() throws InterruptedException {
        return new File(Framework.getDataDirectory() + "/cache");
    }

    public static boolean isPersistent() throws InterruptedException {

        return !(new File(getCacheDirectory() + "/.delay")).exists();
    }

    static void setPersistent(boolean value) {
       try {

           if (!value) {
               (new File(getCacheDirectory() + "/.delay")).writeAllBytes(new byte[0]);
           }
           else {
               (new File(getCacheDirectory() + "/.delay")).delete();
           }
       }
       catch (Exception e) {
       }
    }

    static Long getLastCleanupStamp() {
        return lastCleanupStamp;
    }

    static <TIndex, TData> long allocate(Class<TIndex> index, Class<TData> data) {

        if (index == null || data == null) throw new IllegalArgumentException();

        String key = "index:{" + index.getName() + "}, data:{" + data.getName() + "}";

        synchronized (_caches) {
            if (!_caches.containsKey(key)) {
                setPersistent(false);
                _caches.put(key, ++_currentFileIndex);
            }

            return _caches.get(key);
        }
    }

    public static Serializer getDefaultSerializer() {
        return _defaultSerializer;
    }

    public static void setDefaultSerializer(Serializer serializer) {
        if (serializer != null) {
            _defaultSerializer = serializer;
        }
    }

    public static int getCacheCount() {
        synchronized (_caches) {
            return _caches.size();
        }
    }

    public static CachingMode getMode() {
        return StoredStates.getInt(CacheController.class, "caching-mode", 0) == 0 ? CachingMode.Dynamic : CachingMode.Static;
    }

    public static void setMode(CachingMode mode) {
        StoredStates.set(CacheController.class, "caching-mode", (int)(mode == CachingMode.Static ? 0 : 1));
    }

    public static void clearAll(long stamp) {
        lastCleanupStamp = stamp;
        try { onCleanup.invoke(signature, lastCleanupStamp); } catch (IllegalAccessException e) { }
    }

    public static void clearAll() {
        clearAll(System.currentTimeMillis());
    }
}
