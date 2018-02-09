package kmine.level.format

class Chunk(chunkX: Int, chunkZ: Int,
            subChunks: List<String> = emptyList(),
            entities: List<String> = emptyList(),
            tiles: List<String> = emptyList(),
            biomeIds: String = "",
            heightMap: Array<Int> = emptyArray(),
            var extraData: List<Int> = emptyList()) {
    companion object {
        const val MAX_SUBCHUNKS = 16
    }
    //this.height = Chunk.MAX_SUBCHUNKS
    /** @var int */
    var x: Int = chunkX

    /** @var int */
    var z: Int = chunkZ

    /** @var bool */
    protected var hasChanged = false

    /** @var bool */
    protected var isInit = false

    /** @var bool*/
    protected var lightPopulated = false
    /** @var bool */
    protected var terrainGenerated = false
    /** @var bool */
    protected var terrainPopulated = false

    /** @var int */
    var height = Chunk.MAX_SUBCHUNKS

    /** @var \SplFixedArray|SubChunkInterface[] */
    protected var subChunks: List<SubChunkInterface>

    private var emptySubChunk = EmptySubChunk()

    /** @var Tile[] */
    protected var tiles: List<Tile> = emptyList()
    /** @var Tile[] */
    protected var tileList: List<Tile> = emptyList()

    /** @var Entity[] */
    protected var entities: List<Entity> = emptyList()

    /** @var int[] */
    protected var heightMap: Array<Int> = emptyArray()

    /** @var string */
    protected var biomeIds: String = ""

    /** @var CompoundTag[] */
    protected var NBTtiles: List<CompoundTag> = tiles

    /** @var CompoundTag[] */
    protected var NBTentities: List<CompoundTag> = entities

    init {
        this.subChunks = List(this.height, { EmptySubChunk() })
        if (heightMap.count() == 256)
            this.heightMap = heightMap
        else {
            assert(heightMap.count() == 0, { "Wrong HeightMap value count, expected 256, got ${heightMap.count()}" })
            val `val` = this.height * 16
            this.heightMap.fill(`val`, 0, 256)
        }
        if (biomeIds.length == 256)
            this.biomeIds = biomeIds
        else {
            assert(biomeIds.count() == 0, { "Wrong HeightMap value count, expected 256, got ${biomeIds.count()}" })
            this.biomeIds = "\\x00".repeat(256)
        }
    }

    fun getFullBlock(x: Int, y: Int, z: Int): Int = this.getSubChunk(y.shr(4), true)
            .getFullBlock(x, y and 0x0f, z)

    fun setBlock(x: Int, y: Int, z: Int, blockId: Int? = null, meta: Int? = null): Boolean {
        if (this.getSubChunk(y.shr(4), true).setBlock(x, y and 0x0f, z,
                        if (blockId != null) blockId and 0xff else null,
                        if (meta != null) meta and 0xff else null)) {
            return true
        }
        return false
    }

    fun getBlockId(x:Int, y:Int, z:Int): Int = this.getSubChunk(y.shr(4)).getBlockId(x, y and 0x0f, z)

    fun getSubChunk(y: Int, generateNew: Boolean = false): SubChunkInterface {
        if (y < 0 || y >= this.height) return emptySubChunk
        else if (generateNew && subChunks[y] is EmptySubChunk)
            subChunks.getOrElse(y, defaultValue = SubChunks())

        return subChunks[y]
    }

}