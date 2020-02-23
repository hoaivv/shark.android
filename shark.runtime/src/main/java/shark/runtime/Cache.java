package shark.runtime;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import shark.io.Binary;
import shark.io.File;
import shark.runtime.serialization.Serializer;
import shark.utils.Log;

public class Cache<TIndex, TData> {

    private static HashMap<String, Cache> instances = new HashMap<>();

    private long identifer = 0;
    private boolean ready = false;

    private Serializer serialier = CacheController.getDefaultSerializer();

    private HashMap<TIndex, Object> entries = new HashMap<>();
    private HashMap<TIndex, Long> entryFileIndexes = new HashMap<>();
    private HashMap<TIndex, WeakReference<CacheEntry<TIndex, TData>>> unsettled = new HashMap<>();

    private long currentFileIndex = 0;
    private Long lastEntryModifiedUtc = null;
    private CachingMode mode;

    private Class<TIndex> indexClass;
    private Class<TData> dataClass;

    private Cache(String key, final Class<TIndex> index, final Class<TData> data) {

        instances.put(key, this);

        indexClass = index;
        dataClass = data;

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
                    CacheEntry<TIndex, TData> entry = CacheEntry._of(indexClass, dataClass)._get(fileIndex);

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
                catch (InterruptedException e) {
                }
            }
        }

        ready = true;

        try {

            if (CacheController.getLastCleanupStamp() != null) {
                Log.information(this.getClass(), "Cached data is going to be clean by earlier cleanup request");
                clear(CacheController.getLastCleanupStamp());
            }

            CacheController.onCleanup.add(new Action.One<Long>() {
                @Override
                public void run(Long arg) {

                    try {
                        clear(arg);
                    } catch (InterruptedException e) {
                    }
                }
            });

            CacheController.onCommitChangesToStorage.add(_commitChangesToStorage);
            CacheEntry._of(indexClass, dataClass).onEntryModified.add(_entryModified);
        } catch (InterruptedException e) {
        }
    }


    private Action.One<CacheEntryModifiedEventArgs<TIndex, TData>> _entryModified = new Action.One<CacheEntryModifiedEventArgs<TIndex, TData>>() {
        @Override
        public void run(CacheEntryModifiedEventArgs<TIndex, TData> arg) {

            try {

                switch (arg.getAction()) {
                    case Create:

                        synchronized (entries) {
                            switch (mode) {
                                case Static:
                                    entries.put(arg.getEntry().getIndex(), arg.getEntry());
                                    break;

                                default:
                                    entries.put(arg.getEntry().getIndex(), new WeakReference<CacheEntry<TIndex, TData>>(arg.getEntry()));
                                    break;
                            }

                            entryFileIndexes.put(arg.getEntry().getIndex(), arg.getEntry().getFileIndex());
                            unsettled.remove(arg.getEntry().getIndex());
                        }

                        break;

                    case Delete:

                        synchronized (entries) {
                            entries.remove(arg.getEntry().getIndex());
                            entryFileIndexes.remove(arg.getEntry().getIndex());
                        }

                        break;
                }
            }
            catch (IOException e) {
            }
        }
    };

    private Long storedLastModified = null;

    private Function<Boolean> _commitChangesToStorage = new Function<Boolean>() {
        @Override
        public Boolean run() {

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
                            Binary.write(stream, (long) current);
                        } finally {
                            if (stream != null) stream.close();
                        }
                    } else {
                        if (file.exists() && file.isFile()) file.delete();
                    }

                    storedLastModified = current;

                    return true;
                }
                catch (Exception e) {
                    return false;
                }
            }

            return true;
        }
    };

    void _removeUnsettled(TIndex index) {
        synchronized (entries) {
            unsettled.remove(index);
        }
    }

    public static <TIndex, TData> Cache<TIndex, TData> of(Class<TIndex> index, Class<TData> data) throws InterruptedException {

        String key = "index:{" + index.getName() + "}, data:{" + data.getName() + "}";

        Cache<TIndex, TData> result;

        synchronized (instances) {

            return  instances.containsKey(key) ? instances.get(key) : new Cache<TIndex, TData>(key, index, data);
        }
    }

    public int getEntryCount() {
        synchronized (entries) {
            return entries.size();
        }
    }

    public File getCacheDirectory() throws InterruptedException {
        return new File(CacheController.getCacheDirectory() + "/" + identifer);
    }

    public boolean isSpaceAllocated() {
        return identifer > 0;
    }

    public boolean isReady() {
        return ready;
    }

    public long getLastEntryModifiedUtc() {
        synchronized (entries) {
            if (isSpaceAllocated() && lastEntryModifiedUtc == null) {

                try {
                    File path = new File(getCacheDirectory() + "/.info");

                    if (path.exists() && path.isFile()) {

                        FileInputStream stream = null;
                        try {
                            lastEntryModifiedUtc = Binary.readLong(stream);
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

    void setLastEntryModifiedUtc(long value) {
        synchronized (entries) {
            if (isSpaceAllocated() && (lastEntryModifiedUtc == null || lastEntryModifiedUtc < value)) {
                CacheController.setPersistent(false);
                lastEntryModifiedUtc = value;
            }
        }
    }

    public CacheEntry<TIndex, TData>[] entries() throws InterruptedException {
        ArrayList<CacheEntry<TIndex, TData>> results = new ArrayList<>();

        synchronized (entries) {

            for (TIndex index : entries.keySet()) {
                CacheEntry<TIndex, TData> entry = get(index);
                results.add(entry);
            }
        }
        return results.toArray(CacheEntry._of(indexClass, dataClass)._sample);
    }

    public TIndex[] indexes() {
        synchronized (entries) {
            return entries.keySet().toArray((TIndex[])Array.newInstance(indexClass, 0));
        }
    }

    public Serializer getSerialier() {
        return serialier;
    }

    public void setSerialier(Serializer value) {
        if (value != null) serialier = value;
    }

    public boolean hasAll(TIndex... indexes) {
        if (!isSpaceAllocated()) return false;

        synchronized (entries) {
            for (TIndex index : indexes) if (!has(index)) return false;
        }

        return true;
    }

    public boolean hasAny(TIndex... indexes) {

        if (!isSpaceAllocated()) return false;

        synchronized (entries) {
            for (TIndex index: indexes) if (has(index)) return true;
        }

        return false;
    }

    public boolean has(TIndex index) {
        if (index == null || !isSpaceAllocated()) return false;
        synchronized (entries) {
            return entries.containsKey(index);
        }
    }

    public CacheEntry<TIndex, TData> getOrCreate(TIndex index) throws InterruptedException {

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
                            entry = CacheEntry._of(indexClass, dataClass)._get(entryFileIndexes.get(index));
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
                    entry = CacheEntry._of(indexClass, dataClass)._create(index, currentFileIndex);
                    unsettled.put(index, new WeakReference<CacheEntry<TIndex, TData>>(entry));
                }

                return entry;
            }
        }
    }

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
                        entry = CacheEntry._of(indexClass, dataClass)._get(entryFileIndexes.get(index));
                        entries.put(index, new WeakReference<CacheEntry<TIndex, TData>>(entry));
                    }

                    return entry;
            }
        }
    }

    public CacheEntry<TIndex, TData>[] entries(TIndex... indexes) throws InterruptedException {

        ArrayList<CacheEntry<TIndex, TData>> buffer = new ArrayList<>();

        for (TIndex index : indexes) {
            if (index == null) continue;
            CacheEntry<TIndex, TData> entry = get(index);
            if (entry != null) buffer.add(entry);
        }

        return buffer.toArray(CacheEntry._of(indexClass, dataClass)._sample);

    }

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

    public TData retrive(TIndex index) throws InterruptedException {
        return retrive(index, null);
    }

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

    public long getCreationStampUtc(TIndex index) throws InterruptedException {
        return getCreationStampUtc(index, System.currentTimeMillis());
    }

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

    public long getLastModifiedUtc(TIndex index) throws InterruptedException {
        return getLastModifiedUtc(index, 0);
    }

    public boolean update(TIndex index, TData data, long lastModifiedUtc) throws InterruptedException {

        if (index == null) return false;
        CacheEntry<TIndex, TData> entry = getOrCreate(index);
        return entry != null && entry.update(data, lastModifiedUtc);
    }

    public boolean update(TIndex index, TData data) throws InterruptedException {
        return update(index, data, System.currentTimeMillis());
    }

    public boolean delete(TIndex index, long actionStampUtc) throws InterruptedException {

        if (index == null) return false;

        CacheEntry<TIndex, TData> entry = get(index);

        return entry != null && entry.delete(actionStampUtc);
    }

    public boolean delete(TIndex index) throws InterruptedException {
        return delete(index, System.currentTimeMillis());
    }

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

     public void clear() throws InterruptedException {
        clear(System.currentTimeMillis());
     }
}
