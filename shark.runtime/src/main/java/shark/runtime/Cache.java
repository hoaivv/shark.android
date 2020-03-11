package shark.runtime;

import android.annotation.SuppressLint;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import shark.delegates.Action1;
import shark.io.File;
import shark.io.primitive;
import shark.runtime.events.ActionEvent;
import shark.runtime.serialization.Serializer;
import shark.utils.Log;

/**
 * Provides access to caches of Shark Caching System.
 * @param <TIndex> type of caching index
 * @param <TData> type of caching data
 */
@SuppressWarnings("WeakerAccess")
public class Cache<TIndex, TData> {

    private static final HashMap<String, Cache> instances = new HashMap<>();

    private final long identifier;
    private boolean ready = false;

    private Serializer serializer = CacheController.getDefaultSerializer();

    private final HashMap<TIndex, Object> entries = new HashMap<>();
    private final HashMap<TIndex, Long> entryFileIndexes = new HashMap<>();
    private final HashMap<TIndex, WeakReference<CacheEntry<TIndex, TData>>> unsettled = new HashMap<>();
    @SuppressLint("UseSparseArrays")
    private final HashMap<Long, CacheEntry<TIndex, TData>> pendingEntries = new HashMap<>();

    private long currentFileIndex = 0;
    private Long lastEntryModifiedUtc = null;
    private CachingMode mode;

    final Class<TIndex> _indexClass;
    final Class<TData> _dataClass;

    private final CacheEntry<TIndex, TData>[] arraySample;

    @SafeVarargs
    private Cache(String key, final Class<TIndex> index, final Class<TData> data, CacheEntry<TIndex, TData>... sample) {

        arraySample = sample;
        _indexClass = index;
        _dataClass = data;

        identifier = CacheController.allocate(index, data);

        if (identifier == 0) {
            Log.error(this.getClass(), "Caching space is not allocated");
            return;
        }

        File dir = getCacheDirectory();

        if (!dir.exists() && !dir.mkdirs()) {
                Log.error(this.getClass(), "Could not create cache directory");
        }

        mode = CacheController.getMode();

        synchronized (entries) {

            for (String file : dir.list()) {

                try {
                    long fileIndex = Long.parseLong(file.split("\\.")[0]);
                    CacheEntry<TIndex, TData> entry = new CacheEntry<>(this, fileIndex);

                    if (entry._load()) {
                        switch (mode) {
                            case Static:
                                entries.put(entry.getIndex(), entry);
                                break;
                            default:
                                entries.put(entry.getIndex(), new WeakReference<TData>(null));
                                break;

                        }

                        entryFileIndexes.put(entry.getIndex(), fileIndex);
                        currentFileIndex = Math.max(fileIndex, currentFileIndex);
                    } else {
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            new File(dir + "/" + file).delete();
                        } catch (Exception ignored) {

                        }
                    }
                } catch (NumberFormatException e) {

                    try {
                        //noinspection ResultOfMethodCallIgnored
                        new File(dir + "/" + file).delete();
                    } catch (Exception ignored) {
                    }
                } catch (IOException ignored) {
                }
            }
        }

        ready = true;

        if (CacheController.getLastCleanupStamp() != null) {
            Log.information(this.getClass(), "Cached data is going to be clean by earlier cleanup request");
            clear(CacheController.getLastCleanupStamp());
        }

        CacheController.onCleanup.add(this::clear);

        CacheController.onCommitChangesToStorage.add(this::commitCacheChangesToStorage);
        CacheController.onCommitChangesToStorage.add(this::commitEntryChangesToStorage);

        instances.put(key, this);
    }

    private Long storedLastModified = null;

    private boolean commitCacheChangesToStorage() {

        Long current = lastEntryModifiedUtc;

        //noinspection NumberEquality
        if (storedLastModified != current) {
            try {

                File dir = getCacheDirectory();
                if (!dir.exists() && !dir.mkdirs()) return false;

                File file = new File(dir + "/.info");

                if (current != null) {

                    try (FileOutputStream stream = new FileOutputStream(file)) {
                        primitive.write(stream, current);
                    }
                } else {
                    if (file.exists() && file.isFile()) //noinspection ResultOfMethodCallIgnored
                        file.delete();
                }

                storedLastModified = current;

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    private boolean commitEntryChangesToStorage() {
        Long[] indexes;

        synchronized (pendingEntries) {
            indexes = pendingEntries.keySet().toArray(new Long[0]);
        }

        for (long index : indexes) {

            CacheEntry<TIndex, TData> entry;
            synchronized (pendingEntries) {
                entry = pendingEntries.get(index);
            }

            if (entry == null) {
                try {
                    File file = new File(getCacheDirectory() + "/" + index + ".data");

                    if (file.exists() && file.isFile() && !file.delete()) break;
                    synchronized (pendingEntries) {
                        if (pendingEntries.get(index) == null) pendingEntries.remove(index);
                    }
                } catch (Exception e) {
                    break;
                }
            } else {
                try {
                    long savingVersion = entry.version;

                    if (entry._save()) {
                        synchronized (pendingEntries) {
                            if (pendingEntries.get(index) == entry && entry.version == savingVersion)
                                pendingEntries.remove(index);
                        }
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }

        synchronized (pendingEntries) {
            return pendingEntries.size() < 1;
        }
    }

    /**
     * Marks an entry as freed and could be collected by GC. Cache entry instances, allocated but
     * not saved could be freed.
     * @param index index of an entry to be freed
     */
    void _free(TIndex index) {
        synchronized (entries) {
            unsettled.remove(index);
        }
    }

    /**
     * Notifies that a modification had been made to an entry and directs the cache to commit the
     * modification to storage.
     * @param entry modified entry
     * @param action action committed
     * @param actionStampUtc timestamp of the action
     */
    void _modify(CacheEntry<TIndex, TData> entry, CacheEntryAction action, long actionStampUtc) {

        setLastEntryModifiedUtc(actionStampUtc);

        synchronized (pendingEntries) {
            CacheController.setPersistent(false);
            pendingEntries.put(entry.getFileIndex(), action == CacheEntryAction.Delete ? null : entry);
        }

        try {

            switch (action) {
                case Create:

                    synchronized (entries) {
                        switch (mode) {
                            case Static:
                                entries.put(entry.getIndex(), entry);
                                break;

                            default:
                                entries.put(entry.getIndex(), new WeakReference<>(entry));
                                break;
                        }

                        entryFileIndexes.put(entry.getIndex(), entry.getFileIndex());
                        unsettled.remove(entry.getIndex());
                    }

                    break;

                case Delete:

                    synchronized (entries) {
                        entries.remove(entry.getIndex());
                        entryFileIndexes.remove(entry.getIndex());
                    }

                    break;
            }
        } catch (IOException ignored) {
        }

        //noinspection unchecked,ConstantConditions
        onEntryModifiedInvoker.run(new CacheEntryModifiedEventArgs(entry, action));
    }

    /**
     * Triggers whenever a cache entry of current cache type is created/edited/deleted
     */
    public final ActionEvent<CacheEntryModifiedEventArgs<TIndex, TData>> onEntryModified = new ActionEvent<>();
    private final Action1<CacheEntryModifiedEventArgs<TIndex, TData>> onEntryModifiedInvoker = ActionEvent.getInvoker(onEntryModified);

    /**
     * Gets a specified type of cache.
     * @param index class of the cache index
     * @param data class of the cache data
     * @param <TIndex> type of cache index
     * @param <TData> type of cache data
     * @return instance of {@link Cache} via {@link Promise} which provides access to the specified
     * cache type
     *
     * @exception InterruptedException throws if the calling thread is interrupted be for the
     * cache initialisation is completed
     */
    public static <TIndex, TData> Promise<Cache<TIndex, TData>> get(Class<TIndex> index, Class<TData> data) throws InterruptedException {

        String key = "index:{" + index.getName() + "}, data:{" + data.getName() + "}";

        Promise<Cache<TIndex, TData>> promise = new Promise<>();
        Action1<Cache<TIndex, TData>> resolver = Promise.getResolver(promise);

        try {
            return promise;
        }
        finally {

            boolean pass = false;

            synchronized (instances) {
                if (instances.containsKey(key)) {
                    //noinspection unchecked,ConstantConditions,ConstantConditions
                    resolver.run(instances.get(key));
                    pass = true;
                }
            }

            if (!pass) {

                Parallel.queue(() -> {

                    synchronized (instances) {

                        //noinspection unchecked,ConstantConditions
                        resolver.run(instances.containsKey(key) ? instances.get(key) : new Cache<>(key, index, data));
                    }
                });
            }
        }
    }

    /**
     * Gets the number of entries allocated to the cache
     * @return number of allocated entries
     */
    public int size() {
        synchronized (entries) {
            return entries.size();
        }
    }

    /**
     * Gets the directory where the caching data of the cache is stored. This method blocks the
     * calling thread until {@link shark.Framework} is started.
     * @return Directory where the caching data of the cache is stored
     *
     * @exception RuntimeException throws if Shark is not initialised
     */
    public File getCacheDirectory() {
        return new File(CacheController.getCacheDirectory() + "/" + identifier);
    }

    /**
     * Indicates whether the cache is allocated a storage space to store its data or not
     * @return true if the cache is allocated; otherwise false
     */
    public boolean isSpaceAllocated() {
        return identifier > 0;
    }

    /**
     * Indicates whether the cache is initialised and ready to be used or not
     * @return true if the cache is ready to be used; otherwise false
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Gets the time stamp, at which the cache data is last modified
     * @return unix time stamp at which the cache data is last modified
     */
    public long getLastEntryModifiedUtc() {
        synchronized (entries) {
            if (isSpaceAllocated() && lastEntryModifiedUtc == null) {

                try {
                    File path = new File(getCacheDirectory() + "/.info");

                    if (path.exists() && path.isFile()) {

                        try (FileInputStream stream = new FileInputStream(path)) {
                            lastEntryModifiedUtc = primitive.readLong(stream);
                        }
                    }
                }
                catch (Exception ignored) {
                }
            }
        }

        return lastEntryModifiedUtc == null ? 0 : (long) lastEntryModifiedUtc;
    }

    private void setLastEntryModifiedUtc(long value) {
        synchronized (entries) {
            if (isSpaceAllocated() && (lastEntryModifiedUtc == null || lastEntryModifiedUtc < value)) {
                CacheController.setPersistent(false);
                lastEntryModifiedUtc = value;
            }
        }
    }

    /**
     * Gets all allocated entries of the cache. This method will block the calling thread until
     * {@link shark.Framework} is started
     * @return array of allocated cache entries
     */
    public CacheEntry<TIndex, TData>[] entries() {
        ArrayList<CacheEntry<TIndex, TData>> results = new ArrayList<>();

        synchronized (entries) {

            for (TIndex index : entries.keySet()) {
                CacheEntry<TIndex, TData> entry = get(index);
                results.add(entry);
            }
        }
        return results.toArray(arraySample);
    }

    /**
     * Gets all allocated index of the cache.
     * @return array of allocated index of the cache
     */
    public TIndex[] indexes() {
        synchronized (entries) {
            //noinspection unchecked
            return entries.keySet().toArray((TIndex[])Array.newInstance(_indexClass, 0));
        }
    }

    /**
     * Gets the serializer used to serialize/deserialize caching data
     * @return an instance of {@link Serializer}
     */
    public Serializer getSerializer() {
        return serializer;
    }

    /**
     * Gets the serializer used to serialize/deserialize caching data
     * @param value serializer to be used
     */
    public void setSerializer(Serializer value) {
        if (value != null) serializer = value;
    }

    /**
     * Checks whether all of the specified indexes are allocated or not
     * @param indexes indexes to be checked
     * @return true if all of the provided indexes are allocated; otherwise false
     */
    @SafeVarargs
    public final boolean hasAll(TIndex... indexes) {
        if (!isSpaceAllocated()) return false;

        synchronized (entries) {
            for (TIndex index : indexes) if (!has(index)) return false;
        }

        return true;
    }

    /**
     * Checks whether any of the specified indexes is allocated or not
     * @param indexes indexes to be checked
     * @return true if any of the provided indexes is allocated; otherwise false
     */
    @SafeVarargs
    public final boolean hasAny(TIndex... indexes) {

        if (!isSpaceAllocated()) return false;

        synchronized (entries) {
            for (TIndex index: indexes) if (has(index)) return true;
        }

        return false;
    }

    /**
     * Checks whether a specified index is allocated or not
     * @param index index to be checked
     * @return true if the provided index is allocated; otherwise false
     */
    public boolean has(TIndex index) {
        if (index == null || !isSpaceAllocated()) return false;
        synchronized (entries) {
            return entries.containsKey(index);
        }
    }

    /**
     * Gets a specified entry if it exists; otherwise create an entry. This method will block the
     * calling thread until {@link shark.Framework} is started
     * @param index index of the entry
     * @return an instance of {@link CacheEntry} associated with the provided index
     */
    public CacheEntry<TIndex, TData> getOrCreate(TIndex index) {

        if (index == null) throw new IllegalArgumentException("index is null");
        if (!isSpaceAllocated()) throw new UnsupportedOperationException("caching space is not allocated");

        synchronized (entries) {
            if (entries.containsKey(index)) {
                switch (mode) {
                    case Static:
                        //noinspection unchecked
                        return (CacheEntry<TIndex, TData>) entries.get(index);
                    default:

                        @SuppressWarnings("unchecked") WeakReference<CacheEntry<TIndex, TData>> reference = (WeakReference<CacheEntry<TIndex, TData>>) entries.get(index);
                        @SuppressWarnings("ConstantConditions") CacheEntry<TIndex, TData> entry = reference.get();

                        if (entry == null) {
                            //noinspection ConstantConditions
                            entry = new CacheEntry<>(this, entryFileIndexes.get(index));
                            entries.put(index, new WeakReference<>(entry));
                        }

                        return entry;
                }
            }
            else {
                if (!unsettled.containsKey(index)) unsettled.put(index, new WeakReference<>(null));

                @SuppressWarnings("ConstantConditions") CacheEntry<TIndex, TData> entry = unsettled.get(index).get();

                if (entry == null) {
                    currentFileIndex++;
                    entry = new CacheEntry<>(this, index, currentFileIndex);
                    unsettled.put(index, new WeakReference<>(entry));
                }

                return entry;
            }
        }
    }

    /**
     * Gets a specified entry. This method will block the calling thread until the
     * {@link shark.Framework} is started
     * @param index index of the entry
     * @return an instance of {@link CacheEntry} if succeed; otherwise null
     */
    public CacheEntry<TIndex,TData> get(TIndex index) {

        if (index == null) throw new IllegalArgumentException("index is null");

        synchronized (entries) {

            if (!entries.containsKey(index)) return null;

            switch (mode) {
                case Static:

                    //noinspection unchecked
                    return (CacheEntry<TIndex, TData>) entries.get(index);

                default:

                    @SuppressWarnings("unchecked") WeakReference<CacheEntry<TIndex, TData>> reference = (WeakReference<CacheEntry<TIndex, TData>>) entries.get(index);
                    @SuppressWarnings("ConstantConditions") CacheEntry<TIndex, TData> entry = reference.get();

                    if (entry == null) {
                        //noinspection ConstantConditions
                        entry = new CacheEntry<>(this, entryFileIndexes.get(index));
                        entries.put(index, new WeakReference<>(entry));
                    }

                    return entry;
            }
        }
    }

    /**
     * Gets a subset of entries. This method will block the calling thread until
     * {@link shark.Framework} is started
     * @param indexes indexes of the entries to be retrieved
     * @return an array of the requested entries
     */
    @SafeVarargs
    public final CacheEntry<TIndex, TData>[] entries(TIndex... indexes) {

        ArrayList<CacheEntry<TIndex, TData>> buffer = new ArrayList<>();

        for (TIndex index : indexes) {
            if (index == null) continue;
            CacheEntry<TIndex, TData> entry = get(index);
            if (entry != null) buffer.add(entry);
        }

        return buffer.toArray(arraySample);

    }

    /**
     * Retrieves data of a specified entry. This method blocks the calling thread until
     * {@link shark.Framework} is started.
     * @param index index of the entry
     * @param onFailed data to be returned if entry data could not be retrieved
     * @return data of the entry if succeed; otherwise provided onFailed value
     */
    public TData retrieve(TIndex index, TData onFailed) {

        if (index == null) return onFailed;

        CacheEntry<TIndex, TData> entry = getOrCreate(index);

        try {
            return entry == null || !entry._load() || !has(index) ? onFailed : entry.getData();
        }
        catch (IOException e) {
            return onFailed;
        }
    }

    /**
     * Retrieves data of an entry. This method blocks the calling thread until
     * {@link shark.Framework} is started
     * @param index index of the entry
     * @return data of the entry if succeed; otherwise null
     */
    public TData retrieve(TIndex index) {
        return retrieve(index, null);
    }

    /**
     * Gets timestamp, at which a specified entry is created. This method will block the calling
     * thread until {@link shark.Framework} is started
     * @param index index of the entry
     * @param onFailed timestamp to be returned if the operation is failed
     * @return creation timestamp of the entry is succeed; otherwise provided onFailed value
     */
    public long getCreationStampUtc(TIndex index, long onFailed) {

        if (index == null) return onFailed;

        CacheEntry<TIndex, TData> entry = getOrCreate(index);

        try {
            return entry == null || !entry._load() || !has(index) ? onFailed : entry.getCreationStampUtc();
        }
        catch (IOException e) {
            return onFailed;
        }
    }

    /**
     * Gets timestamp, at which a specified entry is created. This method will block the calling
     * thread until {@link shark.Framework} is started
     * @param index index of the entry
     * @return creation timestamp of the entry is succeed; otherwise current system timestamp
     */
    public long getCreationStampUtc(TIndex index) {
        return getCreationStampUtc(index, System.currentTimeMillis());
    }

    /**
     * Gets timestamp, at which a specified entry is last modified. This method blocks the
     * calling thread until {@link shark.Framework} is started
     * @param index index of the entry
     * @param onFailed timestamp to be returned if the operation is failed
     * @return modification timestamp of the entry is succeed; otherwise provided onFailed value
     */
    public long getLastModifiedUtc(TIndex index, long onFailed) {
        if (index == null) return onFailed;

        CacheEntry<TIndex, TData> entry = getOrCreate(index);

        try {
            return entry == null || !entry._load() || !has(index) ? onFailed : entry.getLastModifiedUtc();
        }
        catch (IOException e) {
            return onFailed;
        }
    }

    /**
     * Gets timestamp, at which a specified entry is last modified. This method blocks the
     * calling thread until {@link shark.Framework} is started.
     * @param index index of the entry
     * @return modification timestamp of the entry is succeed; otherwise 0
     */
    public long getLastModifiedUtc(TIndex index) {
        return getLastModifiedUtc(index, 0);
    }

    /**
     * Updates data of a specified entry. If the entry does not exists it will be created. This
     * method blocks calling thread until {@link shark.Framework} is started
     * @param index index of the entry
     * @param data data to be set to the entry
     * @param lastModifiedUtc timestamp to be recorded as the last modification time of the entry
     * @return true if succeed; otherwise false
     */
    public boolean update(TIndex index, TData data, long lastModifiedUtc) {

        if (index == null) return false;
        CacheEntry<TIndex, TData> entry = getOrCreate(index);
        return entry != null && entry.update(data, lastModifiedUtc);
    }

    /**
     * Updates data of a specified entry. If the entry does not exists it will be created. This
     * method blocks calling thread until {@link shark.Framework} is started
     * @param index index of the entry
     * @param data data to be set to the entry
     * @return true if succeed; otherwise false
     */
    public boolean update(TIndex index, TData data) {
        return update(index, data, System.currentTimeMillis());
    }

    /**
     * Deletes an entry. This method blocks the calling thread until {@link shark.Framework} is
     * started
     * @param index index of the entry
     * @param actionStampUtc timestamp to be set as cache last modification time
     * @return true if succeed; otherwise false
     */
    public boolean delete(TIndex index, long actionStampUtc) {

        if (index == null) return false;

        CacheEntry<TIndex, TData> entry = get(index);

        return entry != null && entry.delete(actionStampUtc);
    }

    /**
     * Deletes an entry. This method blocks the calling thread until {@link shark.Framework} is
     * started
     * @param index index of the entry
     * @return true if succeed; otherwise false
     */
    public boolean delete(TIndex index) {
        return delete(index, System.currentTimeMillis());
    }

    /**
     * Deletes all entries. This method will block the calling thread until {@link shark.Framework}
     * is started
     * @param actionStampUtc timestamp to be set as cache last modification time
     */
    public void clear(long actionStampUtc) {
        synchronized (entries) {
            lastEntryModifiedUtc = null;

            if (entries.size() > 0) {
                for (TIndex index : entries.keySet()) {
                    get(index).delete(actionStampUtc);
                }
            }
            else {
                setLastEntryModifiedUtc(actionStampUtc);
            }
        }

        Log.information(this.getClass(), "Cache is clean");
     }

    /**
     * Deletes all entries. This method will block the calling thread until {@link shark.Framework}
     * is started
     */
     public void clear() {
        clear(System.currentTimeMillis());
     }
}
