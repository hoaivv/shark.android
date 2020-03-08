package shark.runtime;

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
public class Cache<TIndex, TData> {

    private static HashMap<String, Cache> instances = new HashMap<>();

    private long identifer = 0;
    private boolean ready = false;

    private Serializer serialier = CacheController.getDefaultSerializer();

    private HashMap<TIndex, Object> entries = new HashMap<>();
    private HashMap<TIndex, Long> entryFileIndexes = new HashMap<>();
    private HashMap<TIndex, WeakReference<CacheEntry<TIndex, TData>>> unsettled = new HashMap<>();
    private HashMap<Long, CacheEntry<TIndex, TData>> pendingEntries = new HashMap<>();

    private long currentFileIndex = 0;
    private Long lastEntryModifiedUtc = null;
    private CachingMode mode;

    Class<TIndex> _indexClass;
    Class<TData> _dataClass;

    private CacheEntry<TIndex, TData>[] arraySample;

    private Cache(String key, final Class<TIndex> index, final Class<TData> data, CacheEntry<TIndex, TData>... sample) {

        instances.put(key, this);

        arraySample = sample;
        _indexClass = index;
        _dataClass = data;

        identifer = CacheController.allocate(index, data);

        if (identifer == 0) {
            Log.error(this.getClass(), "Caching space is not allocated");
        }

        File dir;

        try {
            dir = getCacheDirectory();

            if (!dir.exists() && !dir.mkdirs()) {
                Log.error(this.getClass(), "Could not create cache directory");
            }
        } catch (InterruptedException e) {

            Log.error(this.getClass(), "Could not create cache directory");
            return;
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
                            new File(dir + "/" + file).delete();
                        } catch (Exception e) {

                        }
                    }
                }
                catch (NumberFormatException e) {

                    try {
                        new File(dir + "/" + file).delete();
                    }
                    catch (Exception e1) {
                    }
                }
                catch (IOException e) {
                }
            }
        }

        ready = true;

        try {

            if (CacheController.getLastCleanupStamp() != null) {
                Log.information(this.getClass(), "Cached data is going to be clean by earlier cleanup request");
                clear(CacheController.getLastCleanupStamp());
            }

            CacheController.onCleanup.add(new Action1<Long>() {
                @Override
                public void run(Long arg) {

                    try {
                        clear(arg);
                    } catch (InterruptedException e) {
                    }
                }
            });

            CacheController.onCommitChangesToStorage.add(() -> commitCacheChangesToStorage());
            CacheController.onCommitChangesToStorage.add(() -> commitEntryChangesToStorage());
        } catch (InterruptedException e) {
        }
    }

    private Long storedLastModified = null;

    private boolean commitCacheChangesToStorage() {

        Long current = lastEntryModifiedUtc;

        if (storedLastModified != current) {
            try {

                File dir = getCacheDirectory();
                if (!dir.exists() && !dir.mkdirs()) return false;

                File file = new File(dir + "/.info");

                if (current != null) {

                    FileOutputStream stream = null;
                    try {
                        stream = new FileOutputStream(file);
                        primitive.write(stream, (long) current);
                    } finally {
                        if (stream != null) stream.close();
                    }
                } else {
                    if (file.exists() && file.isFile()) file.delete();
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
        } catch (IOException e) {
        }

        onEntryModifiedInvoker.run(new CacheEntryModifiedEventArgs(entry, action));
    }

    /**
     * Triggers whenever a cache entry of current cache type is created/edited/deleted
     */
    public final ActionEvent<CacheEntryModifiedEventArgs<TIndex, TData>> onEntryModified = new ActionEvent<>();
    private Action1<CacheEntryModifiedEventArgs<TIndex, TData>> onEntryModifiedInvoker = ActionEvent.getInvoker(onEntryModified);

    /**
     * Gets a specified type of cache.
     * @param index class of the cache index
     * @param data class of the cache data
     * @param <TIndex> type of cache index
     * @param <TData> type of cache data
     * @return instance of {@link Cache} via {@link Promise} which provides access to the specified
     * cache type
     */
    public static <TIndex, TData> Promise<Cache<TIndex, TData>> get(Class<TIndex> index, Class<TData> data) {

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
                    resolver.run(instances.get(key));
                    pass = true;
                }
            }

            if (!pass) {

                Parallel.queue(() -> {

                    synchronized (instances) {

                        resolver.run(instances.containsKey(key) ? instances.get(key) : new Cache<TIndex, TData>(key, index, data));
                    }
                });
            }
        }
    }

    /**
     * Gets the number of entries allocated to the cache
     * @return
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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public File getCacheDirectory() throws InterruptedException {
        return new File(CacheController.getCacheDirectory() + "/" + identifer);
    }

    /**
     * Indicates whether the cache is allocated a storage space to store its data or not
     * @return true if the cache is allocated; otherwise false
     */
    public boolean isSpaceAllocated() {
        return identifer > 0;
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

                        FileInputStream stream = null;
                        try {
                            lastEntryModifiedUtc = primitive.readLong(stream);
                        }
                        finally {
                            if (stream != null) stream.close();
                        }
                    }
                }
                catch (Exception e) {
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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public CacheEntry<TIndex, TData>[] entries() throws InterruptedException {
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
            return entries.keySet().toArray((TIndex[])Array.newInstance(_indexClass, 0));
        }
    }

    /**
     * Gets the serializer used to serialize/deserialize caching data
     * @return an instance of {@link Serializer}
     */
    public Serializer getSerialier() {
        return serialier;
    }

    /**
     * Gets the serializer used to serialize/deserialize caching data
     * @param value serializer to be used
     */
    public void setSerialier(Serializer value) {
        if (value != null) serialier = value;
    }

    /**
     * Checks whether all of the specified indexes are allocated or not
     * @param indexes indexes to be checked
     * @return true if all of the provided indexes are allocated; otherwise false
     */
    public boolean hasAll(TIndex... indexes) {
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
    public boolean hasAny(TIndex... indexes) {

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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public CacheEntry<TIndex, TData> getOrCreate(TIndex index) {

        if (index == null) throw new IllegalArgumentException("index is null");
        if (!isSpaceAllocated()) throw new UnsupportedOperationException("caching space is not allocated");

        synchronized (entries) {
            if (entries.containsKey(index)) {
                switch (mode) {
                    case Static:
                        return (CacheEntry<TIndex, TData>) entries.get(index);
                    default:

                        WeakReference<CacheEntry<TIndex, TData>> reference = (WeakReference<CacheEntry<TIndex, TData>>) entries.get(index);
                        CacheEntry<TIndex, TData> entry = reference.get();

                        if (entry == null) {
                            entry = new CacheEntry<>(this, entryFileIndexes.get(index));
                            entries.put(index, new WeakReference<CacheEntry<TIndex, TData>>(entry));
                        }

                        return entry;
                }
            }
            else {
                if (!unsettled.containsKey(index)) unsettled.put(index, new WeakReference<CacheEntry<TIndex, TData>>(null));

                CacheEntry<TIndex, TData> entry = unsettled.get(index).get();

                if (entry == null) {
                    currentFileIndex++;
                    entry = new CacheEntry<>(this, index, currentFileIndex);
                    unsettled.put(index, new WeakReference<CacheEntry<TIndex, TData>>(entry));
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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public CacheEntry<TIndex,TData> get(TIndex index) throws InterruptedException{

        if (index == null) throw new IllegalArgumentException("index is null");

        synchronized (entries) {

            if (!entries.containsKey(index)) return null;

            switch (mode) {
                case Static:

                    return (CacheEntry<TIndex, TData>) entries.get(index);

                default:

                    WeakReference<CacheEntry<TIndex, TData>> reference = (WeakReference<CacheEntry<TIndex, TData>>) entries.get(index);
                    CacheEntry<TIndex, TData> entry = reference.get();

                    if (entry == null) {
                        entry = new CacheEntry<>(this, entryFileIndexes.get(index));
                        entries.put(index, new WeakReference<CacheEntry<TIndex, TData>>(entry));
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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public CacheEntry<TIndex, TData>[] entries(TIndex... indexes) throws InterruptedException {

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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public TData retrive(TIndex index, TData onFailed) throws InterruptedException {

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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public TData retrive(TIndex index) throws InterruptedException {
        return retrive(index, null);
    }

    /**
     * Gets timestamp, at which a specified entry is created. This method will block the calling
     * thread until {@link shark.Framework} is started
     * @param index index of the entry
     * @param onFailed timestamp to be returned if the operation is failed
     * @return creation timestamp of the entry is succeed; otherwise provided onFailed value
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public long getCreationStampUtc(TIndex index, long onFailed) throws InterruptedException {

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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public long getCreationStampUtc(TIndex index) throws InterruptedException {
        return getCreationStampUtc(index, System.currentTimeMillis());
    }

    /**
     * Gets timestamp, at which a specified entry is last modified. This method blocks the
     * calling thread until {@link shark.Framework} is started
     * @param index index of the entry
     * @param onFailed timestamp to be returned if the operation is failed
     * @return modification timestamp of the entry is succeed; otherwise provided onFailed value
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public long getLastModifiedUtc(TIndex index, long onFailed) throws InterruptedException {
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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public long getLastModifiedUtc(TIndex index) throws InterruptedException {
        return getLastModifiedUtc(index, 0);
    }

    /**
     * Updates data of a specified entry. If the entry does not exists it will be created. This
     * method blocks calling thread until {@link shark.Framework} is started
     * @param index index of the entry
     * @param data data to be set to the entry
     * @param lastModifiedUtc timestamp to be recorded as the last modification time of the entry
     * @return true if succeed; otherwise false
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public boolean delete(TIndex index, long actionStampUtc) throws InterruptedException {

        if (index == null) return false;

        CacheEntry<TIndex, TData> entry = get(index);

        return entry != null && entry.delete(actionStampUtc);
    }

    /**
     * Deletes an entry. This method blocks the calling thread until {@link shark.Framework} is
     * started
     * @param index index of the entry
     * @return true if succeed; otherwise false
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public boolean delete(TIndex index) throws InterruptedException {
        return delete(index, System.currentTimeMillis());
    }

    /**
     * Deletes all entries. This method will block the calling thread until {@link shark.Framework}
     * is started
     * @param actionStampUtc timestamp to be set as cache last modification time
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
    public void clear(long actionStampUtc) throws InterruptedException {
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
     * @throws InterruptedException throws if the calling thread is interrupted before
     * {@link shark.Framework} is started
     */
     public void clear() throws InterruptedException {
        clear(System.currentTimeMillis());
     }
}
