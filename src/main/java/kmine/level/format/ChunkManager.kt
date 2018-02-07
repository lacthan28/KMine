package kmine.level.format

interface ChunkManager {
    /**
     * Gets the raw block id.
     *
     * @param x : Int
     * @param y : Int
     * @param z : Int
     *
     * @return int 0-255
     */
    fun getBlockIdAt(x : Int, y : Int, z : Int) : int

    /**
     * Sets the raw block id.
     *
     * @param x : Int
     * @param y : Int
     * @param z : Int
     * @param int $id 0-255
     */
    fun setBlockIdAt(x : Int, y : Int, z : Int, int $id)

    /**
     * Gets the raw block metadata
     *
     * @param x : Int
     * @param y : Int
     * @param z : Int
     *
     * @return int 0-15
     */
    fun getBlockDataAt(x : Int, y : Int, z : Int) : int

    /**
     * Sets the raw block metadata.
     *
     * @param x : Int
     * @param y : Int
     * @param z : Int
     * @param int $data 0-15
     */
    fun setBlockDataAt(x : Int, y : Int, z : Int, int $data)

    /**
     * Returns the raw block light level
     *
     * @param x : Int
     * @param y : Int
     * @param z : Int
     *
     * @return int
     */
    fun getBlockLightAt(x : Int, y : Int, z : Int) : int

    /**
     * Sets the raw block light level
     *
     * @param x : Int
     * @param y : Int
     * @param z : Int
     * @param level : Int
     */
    fun setBlockLightAt(x : Int, y : Int, z : Int, level : Int)

    /**
     * Returns the highest amount of sky light can reach the specified coordinates.
     *
     * @param x : Int
     * @param y : Int
     * @param z : Int
     *
     * @return int
     */
    fun getBlockSkyLightAt(x : Int, y : Int, z : Int) : int

    /**
     * Sets the raw block sky light level.
     *
     * @param x : Int
     * @param y : Int
     * @param z : Int
     * @param level : Int
     */
    fun setBlockSkyLightAt(x : Int, y : Int, z : Int, level : Int)

    /**
     * @param chunkX : Int
     * @param chunkZ : Int
     *
     * @return Chunk|null
     */
    fun getChunk(chunkX : Int, chunkZ : Int)

    /**
     * @param int        $chunkX
     * @param int        $chunkZ
     * @param Chunk|null $chunk
     */
    fun setChunk(chunkX : Int, chunkZ : Int, Chunk $chunk = null)

    /**
     * Gets the level seed
     *
     * @return int
     */
    fun getSeed() : int

    /**
     * Returns the height of the world
     * @return int
     */
    fun getWorldHeight() : int

    /**
     * Returns whether the specified coordinates are within the valid world boundaries, taking world format limitations
     * into account.
     *
     * @param float $x
     * @param float $y
     * @param float $z
     *
     * @return bool
     */
    fun isInWorld(float $x, float $y, float $z) : bool
}