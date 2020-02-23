package shark.runtime;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import shark.io.File;
import shark.runtime.serialization.Serializer;
import shark.utils.Log;

public class CacheEntry<TIndex,TData> implements ITypeScopeDistinguishable {

    private static HashMap<String, CacheEntryOperator> instances = new HashMap<>();

    static <TIndex, TData> CacheEntryOperator<TIndex, TData> _of(Class<TIndex> index, Class<TData> data) throws InterruptedException {

        String key = "index:{" + index.getName() + "}, data:{" + data.getName() +"}";

        synchronized (instances) {
            if (!instances.containsKey(key)) instances.put(key, new CacheEntryOperator(index, data));
            return (CacheEntryOperator<TIndex,TData>)instances.get(key);
        }
    }

    private Long creationStampUtc = null;
    private Long lastModifiedUtc = null;
    private TData data;
    private TIndex index;
    private long fileIndex;
    private boolean isLoaded;

    long version = 0;

    private Class<TIndex> indexClass;
    private Class<TData> dataClass;

    CacheEntry(Class<TIndex> tIndex, Class<TData> tData, long fileIndex) {

        indexClass = tIndex;
        dataClass = tData;

        this.fileIndex = fileIndex;
    }

    CacheEntry(Class<TIndex> tIndex, Class<TData> tData, TIndex index, long fileIndex) {

        indexClass = tIndex;
        dataClass = tData;

        this.fileIndex = fileIndex;
        this.index = index;
        creationStampUtc = null;
        lastModifiedUtc = null;
        data = null;
        isLoaded = true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (creationStampUtc == null) Cache.of(indexClass, dataClass)._removeUnsettled(index);
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public long getTypeScopeUniqueIdentifier() {
        return fileIndex;
    }

    public ITypeScopeDistinguishable getTypeScopeUniqueInstance() {
        return this;
    }

    public File getFile() throws InterruptedException {
        return CacheEntry._of(indexClass, dataClass)._getFile(fileIndex);
    }

    public long getFileIndex() {
        return fileIndex;
    }

    public TIndex getIndex() throws IOException {

        synchronized (this) {
            if (!isLoaded && !_load()) throw new IOException("Could not retrive entry");
            return index;
        }
    }

    public TData getData() throws IOException {

        synchronized (this) {
            if (!isLoaded && !_load()) throw new IOException("Could not retrive entry");
            return data;
        }
    }

    public long getCreationStampUtc() throws IOException {

        synchronized (this) {
            if (!isLoaded && !_load()) throw new IOException("Could not retrive entry");
            return creationStampUtc == null ? System.currentTimeMillis() : (long)creationStampUtc;
        }
    }

    public long getLastModifiedUtc() throws IOException {

        synchronized (this) {
            if (!isLoaded && !_load()) throw new IOException("Could not retrive entry");
            return lastModifiedUtc == null ? System.currentTimeMillis() : (long)lastModifiedUtc;
        }
    }

    public boolean isDeletedOrNotInitialized() throws InterruptedException {

        synchronized (this) {
            File file = getFile();
            return file.exists() && file.isFile();
        }
    }

    boolean _load() {

        synchronized (this) {
            if (isLoaded) return true;

            try {
                File file = getFile();
                if (!file.exists() || !file.isFile()) return false;

                FileInputStream stream = null;

                try {
                    Serializer serializer = Cache.of(indexClass, dataClass).getSerialier();
                    stream = new FileInputStream(file);

                    creationStampUtc = serializer.deserializeWithLengthPrefix(stream, Long.class);
                    lastModifiedUtc = serializer.deserializeWithLengthPrefix(stream, Long.class);
                    index = serializer.deserializeWithLengthPrefix(stream, indexClass);
                    data = serializer.deserializeWithLengthPrefix(stream, dataClass);
                }
                finally {
                    if (stream != null) stream.close();
                }

                isLoaded = true;
                return true;

            }
            catch (Exception e) {
                return false;
            }
        }
    }

    boolean save() {

        try {

            File dir = Cache.of(indexClass, dataClass).getCacheDirectory();
            if (!dir.exists() && !dir.mkdirs()) {

                Log.error(this.getClass(), "Could not create cache directory");
                return false;
            }
        }
        catch (Exception e) {
            Log.error(this.getClass(), "Could not create cache directory");
            return false;
        }

        try {
            synchronized (this) {

                Serializer serializer = Cache.of(indexClass, dataClass).getSerialier();
                FileOutputStream stream = null;

                try {
                    stream = new FileOutputStream(getFile());

                    serializer.serializeWithLengthPrefix(stream, creationStampUtc);
                    serializer.serializeWithLengthPrefix(stream, lastModifiedUtc);
                    serializer.serializeWithLengthPrefix(stream, index);
                    serializer.serializeWithLengthPrefix(stream, data);
                }
                finally {
                    if (stream != null) stream.close();
                }

                return true;
            }
        }
        catch (Exception e) {
            return false;
        }
    }

    public boolean update(TData value, long lastModifiedUtc) throws InterruptedException {

        synchronized (this) {

            if (!isLoaded) _load();
            boolean create = creationStampUtc == null;

            if (create) creationStampUtc = lastModifiedUtc;
            this.lastModifiedUtc = lastModifiedUtc;
            data = value;
            version++;

            isLoaded = true;

            Cache.of(indexClass, dataClass).setLastEntryModifiedUtc(lastModifiedUtc);

            synchronized (CacheEntry._of(indexClass, dataClass).pendingEntries) {
                CacheController.setPersistent(false);
                CacheEntry._of(indexClass, dataClass).pendingEntries.put(fileIndex, this);
            }

            try {
                CacheEntry._of(indexClass, dataClass).onEntryModified.invoke(CacheEntry._of(indexClass, dataClass).pendingEntries, new CacheEntryModifiedEventArgs<TIndex, TData>(this, create ? CacheEntryAction.Create : CacheEntryAction.Update));
            }
            catch (IllegalAccessException e){
            }
        }

        return true;
    }

    public boolean update(TData value) throws InterruptedException {
        return update(value, System.currentTimeMillis());
    }

    public boolean delete(long actionStampUtc) throws InterruptedException {

        synchronized (this) {
            Cache.of(indexClass, dataClass).setLastEntryModifiedUtc(actionStampUtc);

            synchronized (CacheEntry._of(indexClass, dataClass).pendingEntries) {
                CacheController.setPersistent(false);
                CacheEntry._of(indexClass, dataClass).pendingEntries.put(fileIndex, null);
            }

            try {
                CacheEntry._of(indexClass, dataClass).onEntryModified.invoke(CacheEntry._of(indexClass, dataClass).pendingEntries, new CacheEntryModifiedEventArgs<TIndex, TData>(this, CacheEntryAction.Delete));
            }
            catch (IllegalAccessException e){
            }
        }

        return true;
    }

    public boolean delete() throws InterruptedException {
        return delete(System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return data == null ? "" : data.toString();
    }
}
