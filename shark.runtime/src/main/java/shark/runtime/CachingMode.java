package shark.runtime;

/**
 * Describes caching modes of Shark Caching System
 */
public enum CachingMode {

    /**
     * Caching data should only be loaded if requested by the application and should be freed from
     * memory if not needed
     */
    Dynamic,

    /**
     * Caching data should be loaded ahead of time and should stay in memory as long as the
     * application is running
     */
    Static
}
