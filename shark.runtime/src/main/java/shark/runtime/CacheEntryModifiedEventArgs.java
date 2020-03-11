package shark.runtime;

/**
 * Argument of cache entry modification event
 * @param <TIndex> type of entry index
 * @param <TData> type of entry data
 */
@SuppressWarnings("WeakerAccess")
public class CacheEntryModifiedEventArgs<TIndex, TData> {

    private final CacheEntry<TIndex, TData> entry;
    private final CacheEntryAction action;

    /**
     * Gets the entry, which is modified
     * @return instance of {@link CacheEntry}
     */
    public CacheEntry<TIndex, TData> getEntry() {
        return entry;
    }

    /**
     * Gets the committed action the the modified entry
     * @return action committed to the modified entry
     */
    public CacheEntryAction getAction() {
        return action;
    }

    CacheEntryModifiedEventArgs(CacheEntry<TIndex, TData> entry, CacheEntryAction action) {
        this.entry = entry;
        this.action = action;
    }

}
