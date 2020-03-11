package shark.runtime;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import shark.io.File;
import shark.runtime.serialization.Serializer;
import shark.utils.Log;

/**
 * Provides access to entries of caches of Shark Caching System
 * @param <TIndex> type of entry index
 * @param <TData> type of entry data
 */
public class CacheEntry<TIndex,TData> implements ITypeScopeDistinguishable {

    private Long creationStampUtc = null;
    private Long lastModifiedUtc = null;
    private TData data;
    private TIndex index;
    private final long fileIndex;
    private boolean isLoaded;

    long version = 0;

    private final Cache<TIndex, TData> cache;

    CacheEntry(Cache<TIndex, TData> cache, long fileIndex) {

        this.cache = cache;
        this.fileIndex = fileIndex;
    }

    CacheEntry(Cache<TIndex, TData> cache, TIndex index, long fileIndex) {

        this.cache = cache;

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
        if (creationStampUtc == null) cache._free(index);
    }

    /**
     * Indicates whether the entry is loaded or not
     * @return true if the entry is loaded; otherwise false
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Gets type scope identifier of the entry
     * @return type scope identifier of the entry
     */
    public long getTypeScopeUniqueIdentifier() {
        return fileIndex;
    }

    /**
     * Gets type scope unique instance ot the entry
     * @return type scope unique instance of the entry
     */
    public ITypeScopeDistinguishable getTypeScopeUniqueInstance() {
        return this;
    }

    /**
     * Gets the file which stored the entry information. This method blocks calling thread until
     * {@link shark.Framework} is started
     * @return file which stored the entry information
     *
     * @exception RuntimeException throws if Shark is not initialised
     */
    @SuppressWarnings("WeakerAccess")
    public File getFile() {
        return new File(cache.getCacheDirectory() + "/" + fileIndex + ".data");
    }

    /**
     * Gets file index of the entry
     * @return file index of the entry
     */
    @SuppressWarnings("WeakerAccess")
    public long getFileIndex() {
        return fileIndex;
    }

    /**
     * Gets entry index
     * @return index of the entry
     * @throws IOException throws if entry information could not be loaded from file
     */
    @SuppressWarnings("WeakerAccess")
    public TIndex getIndex() throws IOException {

        synchronized (this) {
            if (!isLoaded && !_load()) throw new IOException("Could not retrieve entry");
            return index;
        }
    }

    /**
     * Gets entry data
     * @return data of the entry
     * @throws IOException throws if entry information could not be loaded from file
     */
    public TData getData() throws IOException {

        synchronized (this) {
            if (!isLoaded && !_load()) throw new IOException("Could not retrieve entry");
            return data;
        }
    }

    /**
     * Gets entry creation timestamp
     * @return entry creation timestamp
     * @throws IOException throws if entry information could not be loaded from file
     */
    @SuppressWarnings("WeakerAccess")
    public long getCreationStampUtc() throws IOException {

        synchronized (this) {
            if (!isLoaded && !_load()) throw new IOException("Could not retrieve entry");
            return creationStampUtc == null ? System.currentTimeMillis() : creationStampUtc;
        }
    }

    /**
     * Gets entry last modification timestamp
     * @return entry last modification timestamp
     * @throws IOException throws if entry information could not be loaded from file
     */
    @SuppressWarnings("WeakerAccess")
    public long getLastModifiedUtc() throws IOException {

        synchronized (this) {
            if (!isLoaded && !_load()) throw new IOException("Could not retrieve entry");
            return lastModifiedUtc == null ? System.currentTimeMillis() : lastModifiedUtc;
        }
    }

    /**
     * Indicates whether the entry is deleted/not initialised or not. This method block the calling
     * thread until {@link shark.Framework} is started
     * @return true if the entry is deleted/not initialised; otherwise false
     *
     * @exception RuntimeException throws if Shark is not initialised
     */
    public boolean isDeletedOrNotInitialized() {

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
                    Serializer serializer = cache.getSerializer();
                    stream = new FileInputStream(file);

                    creationStampUtc = serializer.deserializeWithLengthPrefix(stream, Long.class);
                    lastModifiedUtc = serializer.deserializeWithLengthPrefix(stream, Long.class);
                    index = serializer.deserializeWithLengthPrefix(stream, cache._indexClass);
                    data = serializer.deserializeWithLengthPrefix(stream, cache._dataClass);
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

    boolean _save() {

        try {

            File dir = cache.getCacheDirectory();
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

                Serializer serializer = cache.getSerializer();

                try (FileOutputStream stream = new FileOutputStream(getFile())) {

                    serializer.serializeWithLengthPrefix(stream, creationStampUtc);
                    serializer.serializeWithLengthPrefix(stream, lastModifiedUtc);
                    serializer.serializeWithLengthPrefix(stream, index);
                    serializer.serializeWithLengthPrefix(stream, data);
                }

                return true;
            }
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Updates entry data. This method block the calling thread until {@link shark.Framework} is
     * started
     * @param value data to be set as entry data
     * @param lastModifiedUtc timestamp to be set as  entry last modification stamp
     * @return true if succeed; otherwise false
     */
    @SuppressWarnings("SameReturnValue")
    public boolean update(TData value, long lastModifiedUtc) {

        synchronized (this) {

            if (!isLoaded) _load();
            boolean create = creationStampUtc == null;

            if (create) creationStampUtc = lastModifiedUtc;
            this.lastModifiedUtc = lastModifiedUtc;
            data = value;
            version++;

            isLoaded = true;

            cache._modify(this, create ? CacheEntryAction.Create : CacheEntryAction.Update, lastModifiedUtc);
        }

        return true;
    }

    /**
     * Updates entry data. This method block the calling thread until {@link shark.Framework} is
     * started
     * @param value data to be set as entry data
     * @return true if succeed; otherwise false
     */
    public boolean update(TData value) {
        return update(value, System.currentTimeMillis());
    }

    /**
     * Deletes the entry. This method block the calling thread until {@link shark.Framework} is
     * started
     * @param actionStampUtc timestamp to be set as current cache type last modification stamp
     * @return true if succeed; otherwise false
     */
    @SuppressWarnings("SameReturnValue")
    public boolean delete(long actionStampUtc) {

        synchronized (this) {
            cache._modify(this, CacheEntryAction.Delete, actionStampUtc);
        }

        return true;
    }

    /**
     * Deletes the entry. This method block the calling thread until {@link shark.Framework} is
     * started
     * @return true if succeed; otherwise false
     */
    public boolean delete() {
        return delete(System.currentTimeMillis());
    }

    /**
     * Represents the entry as a string
     * @return the string which represents the entry
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return data == null ? "" : data.toString();
    }
}
