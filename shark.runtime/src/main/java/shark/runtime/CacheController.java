package shark.runtime;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;

import shark.Framework;
import shark.delegates.Action1;
import shark.delegates.Function1;
import shark.io.File;
import shark.runtime.events.ActionEvent;
import shark.runtime.events.FunctionTrigger;
import shark.runtime.serialization.JsonSerializer;
import shark.runtime.serialization.Serializer;
import shark.utils.Log;

/**
 * Controller of Shark Caching System.
 */
public final class CacheController {

    private class AllocationJson extends HashMap<String, Long> {
    }

    private static HashMap<String, Long> _caches = new HashMap<>();
    private static long _currentFileIndex = 0;
    private static Serializer _defaultSerializer = new JsonSerializer();

    private static Long lastCleanupStamp = null;
    private static int _lastCacheCount = 0;

    static final FunctionTrigger<Boolean> onCommitChangesToStorage = new FunctionTrigger<>();
    private static Function1<Boolean[], Boolean> onCommitChangesToStorageInvoker = FunctionTrigger.getInvoker(onCommitChangesToStorage);

    private static boolean _commitChangesToStorage() {
        synchronized (_caches) {
            try {

                if (_caches.size() == _lastCacheCount) return true;

                File file = new File(getCacheDirectory() + "/.allocation");

                if (file.exists() && (!file.isFile() || !file.delete())) return false;

                FileOutputStream output = null;

                try {
                    output = new FileOutputStream(file);
                    _defaultSerializer.serialize(output, _caches);
                } finally {
                    if (output != null) output.close();
                }

                _lastCacheCount = _caches.size();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    private static void _storingTask() {
        try {

            while (true) {

                setPersistent(onCommitChangesToStorageInvoker.run(new Boolean[] { true }));
                Thread.currentThread().join(100);
            }
        } catch (InterruptedException e) {
        }
    };

    private CacheController() {

        try {

            if (!isPersistent() && getDeleteIfNotPersistent()) getCacheDirectory().delete(true);

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

        onCommitChangesToStorage.add(() -> _commitChangesToStorage());

        Parallel.start(state -> _storingTask(), null, false);
    }

    private static CacheController signature = new CacheController();

    static ActionEvent<Long> onCleanup = new ActionEvent<>();
    private static Action1<Long> onCleanupInvoker = ActionEvent.getInvoker(onCleanup);

    /**
     * Gets directory where all caching data to be stored. This method blocks calling thread until
     * {@link Framework} is started
     * @return directory where all caching data to be stored
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link Framework} is started
     */
    public static File getCacheDirectory() throws InterruptedException {
        return new File(Framework.getDataDirectory() + "/cache");
    }

    /**
     * Indicates whether the allocated caches are persistent or not. This method blocks calling
     * thread until {@link Framework} is started
     * @return directory where all caching data to be stored
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link Framework} is started
     */
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

    /**
     * Gets the default serializer of the caches
     * @return instance of {@link Serializer}
     */
    public static Serializer getDefaultSerializer() {
        return _defaultSerializer;
    }

    /**
     * Sets the default serializer of the caches
     * @param serializer serializer to be set
     */
    public static void setDefaultSerializer(Serializer serializer) {
        if (serializer != null) {
            _defaultSerializer = serializer;
        }
    }

    /**
     * Gets number of allocated caches
     * @return number of allocated caches
     */
    public static int size() {
        synchronized (_caches) {
            return _caches.size();
        }
    }

    /**
     * Gets caching mode of the caches
     * @return caching mode of the caches
     */
    public static CachingMode getMode() {
        return StoredStates.getInt(CacheController.class, "caching-mode", 0) == 0 ? CachingMode.Dynamic : CachingMode.Static;
    }

    /**
     * Sets caching mode of the caches
     * @param mode caching mode to be set
     */
    public static void setMode(CachingMode mode) {
        StoredStates.set(CacheController.class, "caching-mode", (int)(mode == CachingMode.Static ? 0 : 1));
    }

    public static boolean getDeleteIfNotPersistent() {
        return StoredStates.getBoolean(CacheController.class, "delete-if-not-persistent", false);
    }

    public static void setDeleteIfNotPersistent(boolean value) {
        StoredStates.set(CacheController.class, "delete-if-not-persistent", value);
    }

    /**
     * Clears all caches
     * @param stamp timestamp to be set as caches last modification time
     */
    public static void clearAll(long stamp) {
        lastCleanupStamp = stamp;
        onCleanupInvoker.run(lastCleanupStamp);
    }

    /**
     * Clears all caches
     */
    public static void clearAll() {
        clearAll(System.currentTimeMillis());
    }
}
