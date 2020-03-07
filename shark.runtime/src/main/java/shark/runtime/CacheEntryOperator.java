package shark.runtime;

import java.util.HashMap;

import shark.io.File;
import shark.runtime.events.ActionEvent;

public class CacheEntryOperator<TIndex, TData> {

    HashMap<Long, CacheEntry<TIndex, TData>> pendingEntries = new HashMap<>();

    public ActionEvent<CacheEntryModifiedEventArgs<TIndex, TData>> onEntryModified = new ActionEvent<>(pendingEntries);

    private Class<TIndex> indexClass;
    private Class<TData> dataClass;

    CacheEntry<TIndex, TData>[] _sample;

    CacheEntryOperator(Class<TIndex> index, Class<TData> data, CacheEntry<TIndex, TData>... sample){

        indexClass = index;
        dataClass = data;
        _sample = sample;

        CacheController.onCommitChangesToStorage.add(() -> _commitChangesToStorage());
    }

    private boolean _commitChangesToStorage() {
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
                    File file = _getFile(index);

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

    CacheEntry<TIndex, TData> _get(long fileIndex) {

        return new CacheEntry<TIndex, TData>(indexClass, dataClass, fileIndex);
    }

    CacheEntry<TIndex, TData> _create(TIndex index, long fileIndex) {

        return new CacheEntry<TIndex, TData>(indexClass, dataClass, index, fileIndex);
    }

    File _getFile(long fileIndex) throws InterruptedException {
        return new File(Cache.of(indexClass, dataClass).getCacheDirectory() + "/" + fileIndex + ".data");
    }
}
