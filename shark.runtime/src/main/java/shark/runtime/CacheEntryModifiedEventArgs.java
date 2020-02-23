package shark.runtime;

public class CacheEntryModifiedEventArgs<TIndex, TData> {

    private CacheEntry<TIndex, TData> entry;
    private CacheEntryAction action;

    public CacheEntry<TIndex, TData> getEntry() {
        return entry;
    }

    public CacheEntryAction getAction() {
        return action;
    }

    CacheEntryModifiedEventArgs(CacheEntry<TIndex, TData> entry, CacheEntryAction action) {
        this.entry = entry;
        this.action = action;
    }

}
