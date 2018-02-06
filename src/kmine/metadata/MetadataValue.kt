package kmine.metadata

/**
 * @param owningPlugin Plugin
 */
abstract class MetadataValue(var owningPlugin: Plugin) {
    /**
     * Fetches the value of this metadata item.
     *
     * @return mixed
     */
    abstract fun value()

    /**
     * Invalidates this metadata item, forcing it to recompute when next
     * accessed.
     */
    abstract fun invalidate()
}