package kmine.metadata

import kmine.plugin.Plugin
import java.lang.ref.WeakReference

/**
 * @param owningPlugin Plugin
 */
abstract class MetadataValue protected constructor(owningPlugin: Plugin) {
    private var owningPlugin: WeakReference<Plugin> = WeakReference(owningPlugin)

    fun getOwningPlugin() = owningPlugin.get()

    /**
     * Fetches the value of this metadata item.
     *
     * @return mixed
     */
    abstract fun value(): Any

    /**
     * Invalidates this metadata item, forcing it to recompute when next
     * accessed.
     */
    abstract fun invalidate()

}