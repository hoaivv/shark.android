package shark.runtime;

/**
 * Describes actions involved a cache entry
 */
public enum  CacheEntryAction {

    /**
     * A cache entry is created
     */
    Create,

    /**
     * A cache entry is updated
     */
    Update,

    /**
     * A cache entry is deleted
     */
    Delete
}
