package kmine.metadata

import kmine.plugin.Plugin

interface Metadatable {
    /**
     * Sets a metadata value in the implementing object's metadata store.
     *
     * @param metadataKey String
     * @param newMetadataValue MetadataValue
     */
    @Throws(Exception::class)
    fun setMetadata(metadataKey: String, newMetadataValue: MetadataValue)

    /**
     * Returns a list of previously set metadata values from the implementing
     * object's metadata store.
     *
     * @param metadataKey String
     *
     * @return MetadataValue[]
     */
    @Throws(Exception::class)
    fun getMetadata(metadataKey: String): List<MetadataValue>

    /**
     * Tests to see whether the implementing object contains the given
     * metadata value in its metadata store.
     *
     * @param metadataKey String
     *
     * @return Boolean
     */
    @Throws(Exception::class)
    fun hasMetadata(metadataKey: String): Boolean

    /**
     * Removes the given metadata value from the implementing object's
     * metadata store.
     *
     * @param metadataKey String
     * @param owningPlugin Plugin
     */
    @Throws(Exception::class)
    fun removeMetadata(metadataKey: String, owningPlugin: Plugin)
}