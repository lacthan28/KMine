package kmine.level.format

interface SubChunkInterface {
    /**
     * @param checkLight:  Boolean
     * @return bool
     */
    fun isEmpty(checkLight: Boolean = true): Boolean

    /**
     * @param x: Int
     * @param y: Int
     * @param z: Int
     *
     * @return int
     */
    fun getBlockId(x: Int, y: Int, z: Int): Int

    /**
     * @param x: Int
     * @param y: Int
     * @param z: Int
     * @param int $id
     *
     * @return bool
     */
    fun setBlockId(x: Int, y: Int, z: Int, id: Int): Boolean

    /**
     * @param x: Int
     * @param y: Int
     * @param z: Int
     *
     * @return int
     */
    fun getBlockData(x: Int, y: Int, z: Int): Int

    /**
     * @param x: Int
     * @param y: Int
     * @param z: Int
     * @param int $data
     *
     * @return bool
     */
    fun setBlockData(x: Int, y: Int, z: Int, data: Int): Boolean

    /**
     * @param x: Int
     * @param y: Int
     * @param z: Int
     *
     * @return int
     */
    fun getFullBlock(x: Int, y: Int, z: Int): Int

    /**
     * @param int      $x
     * @param int      $y
     * @param int      $z
     * @param int|null $id
     * @param int|null $data
     *
     * @return bool
     */
    fun setBlock(x: Int, y: Int, z: Int, id: Int? = null, data: Int? = null): Boolean

    /**
     * @param x: Int
     * @param y: Int
     * @param z: Int
     *
     * @return int
     */
    fun getBlockLight(x: Int, y: Int, z: Int): Int

    /**
     * @param x: Int
     * @param y: Int
     * @param z: Int
     * @param level: Int
     *
     * @return bool
     */
    fun setBlockLight(x: Int, y: Int, z: Int, level: Int): Boolean

    /**
     * @param x: Int
     * @param y: Int
     * @param z: Int
     *
     * @return int
     */
    fun getBlockSkyLight(x: Int, y: Int, z: Int): Int

    /**
     * @param x: Int
     * @param y: Int
     * @param z: Int
     * @param level: Int
     *
     * @return bool
     */
    fun setBlockSkyLight(x: Int, y: Int, z: Int, level: Int): Boolean

    /**
     * @param x: Int
     * @param z: Int
     *
     * @return int
     */
    fun getHighestBlockAt(x: Int, z: Int): Int

    /**
     * @param x: Int
     * @param z: Int
     *
     * @return String
     */
    fun getBlockIdColumn(x: Int, z: Int): String

    /**
     * @param x: Int
     * @param z: Int
     *
     * @return String
     */
    fun getBlockDataColumn(x: Int, z: Int): String

    /**
     * @param x: Int
     * @param z: Int
     *
     * @return String
     */
    fun getBlockLightColumn(x: Int, z: Int): String

    /**
     * @param x: Int
     * @param z: Int
     *
     * @return String
     */
    fun getBlockSkyLightColumn(x: Int, z: Int): String

    /**
     * @return String
     */
    fun getBlockIdArray(): String

    /**
     * @return String
     */
    fun getBlockDataArray(): String

    /**
     * @return String
     */
    fun getBlockSkyLightArray(): String

    /**
     * @param data: String
     */
    fun setBlockSkyLightArray(data: String)

    /**
     * @return String
     */
    fun getBlockLightArray(): String

    /**
     * @param data: String
     */
    fun setBlockLightArray(data: String)

    /**
     * @return String
     */
    fun networkSerialize(): String

    /**
     * @return String
     */
    fun fastSerialize(): String
}