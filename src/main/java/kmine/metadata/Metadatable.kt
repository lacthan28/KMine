package kmine.metadata

interface Metadatable {
    /**
     * Sets a metadata value in the implementing object's metadata store.
     *
     * @param metadataKey String
     * @param newMetadataValue MetadataValue
     */
    fun setMetadata(metadataKey: String, newMetadataValue: MetadataValue)

    /**
     * Returns a list of previously set metadata values from the implementing
     * object's metadata store.
     *
     * @param metadataKey String
     *
     * @return MetadataValue[]
     */
    fun getMetadata(metadataKey: String): Array<MetadataValue>

    /**
     * Tests to see whether the implementing object contains the given
     * metadata value in its metadata store.
     *
     * @param metadataKey String
     *
     * @return Boolean
     */
    fun hasMetadata(metadataKey: String): Boolean

    /**
     * Removes the given metadata value from the implementing object's
     * metadata store.
     *
     * @param metadataKey String
     * @param owningPlugin Plugin
     */
    fun removeMetadata(metadataKey: String, owningPlugin: Plugin)
}