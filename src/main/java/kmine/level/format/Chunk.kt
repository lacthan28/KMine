package kmine.level.format

class Chunk {
    companion object {
        const val MAX_SUBCHUNKS = 16
    }

    /** @var int */
    private var x: Int = -1

    /** @var int */
    private var z: Int = -1

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
    protected var height = Chunk.MAX_SUBCHUNKS

    /** @var \SplFixedArray|SubChunkInterface[] */
    protected lateinit var subChunks: List<SubChunkInterface>

    /** @var EmptySubChunk */
    protected lateinit var emptySubChunk: EmptySubChunk

    /** @var Tile[] */
    protected var tiles: List<Tile> = emptyList()
    /** @var Tile[] */
    protected var tileList: List<Tile> = emptyList()

    /** @var Entity[] */
    protected var entities: List<Entity> = emptyList()

    /** @var int[] */
    protected var heightMap: List<Int> = emptyList()

    /** @var string */
    protected var biomeIds: String = ""

    /** @var int[] */
    protected var extraData: List<Int> = emptyList()

    /** @var CompoundTag[] */
    protected var NBTtiles: List<CompoundTag> = emptyList()

    /** @var CompoundTag[] */
    protected var NBTentities: List<CompoundTag> = emptyList()

    constructor(chunkX: Int, chunkZ: Int,
                subChunks: List<String> = emptyList(),
                entities: List<String> = emptyList(),
                tiles: List<String> = emptyList(),
                biomeIds: String = "",
                heightMap: List<String> = emptyList(),
                extraData: List<String> = emptyList()) {
        this.x = chunkX
        this.z = chunkZ

//        this.height = Chunk.MAX_SUBCHUNKS

        this.subChunks = List(this.height, SubChunkInterface::class.java)
    }
}