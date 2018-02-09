package kmine.tile

import com.sun.corba.se.spi.copyobject.ObjectCopier
import kmine.Server
import kmine.event.TimingsHandler
import kmine.level.Level
import kmine.level.Position
import kmine.level.format.Chunk
import kmine.math.Vector3
import kmine.nbt.tag.CompoundTag
import sun.plugin.dom.exception.InvalidStateException
import java.util.*

abstract class Tile(level: Level, nbt: CompoundTag) : Position() {

    companion object {
        const val TAG_ID = "id"
        const val TAG_X = "x"
        const val TAG_Y = "y"
        const val TAG_Z = "z"

        const val BANNER = "Banner"
        const val BED = "Bed"
        const val BREWING_STAND = "BrewingStand"
        const val CHEST = "Chest"
        const val ENCHANT_TABLE = "EnchantTable"
        const val ENDER_CHEST = "EnderChest"
        const val FLOWER_POT = "FlowerPot"
        const val FURNACE = "Furnace"
        const val ITEM_FRAME = "ItemFrame"
        const val MOB_SPAWNER = "MobSpawner"
        const val SIGN = "Sign"
        const val SKULL = "Skull"

        /** @var int */
        var tileCount = 1

        /** @var string[] classes that extend Tile */
        private val knownTiles = emptyMap<String, Tile>()
        /** @var string[] */
        private val shortNames = emptyMap<String, String>()

        fun init() {
            Tile.registerTile(Banner::class)
            Tile.registerTile(Bed::class)
            Tile.registerTile(Chest::class)
            Tile.registerTile(EnchantTable::class)
            Tile.registerTile(EnderChest::class)
            Tile.registerTile(FlowerPot::class)
            Tile.registerTile(Furnace::class)
            Tile.registerTile(ItemFrame::class)
            Tile.registerTile(Sign::class)
            Tile.registerTile(Skull::class)
        }

        /**
         * @param type
         * @param level
         * @param nbt
         * @param args
         *
         * @return Tile|null
         */
        fun createTile(type: String, level: Level, nbt: CompoundTag, vararg args: Int): Tile? {
            val cls = Tile.knownTiles[type]!!::class.java
            return if (cls is Tile) cls.getConstructor(cls).newInstance(level, nbt, args) else null
        }

        /**
         * @param className
         *
         * @return bool
         */
        fun registerTile(className: String): Boolean {
            val cls = className::class
            if (cls is Tile && !cls.isAbstract) {
                knownTiles.getOrElse(cls.getShortName(), { this.add(className::class.java) })
                shortNames.getOrElse(className, { cls.getShortName() })
                return true
            }

            return false
        }

        /**
         * Returns the short save name
         * @return string
         */
        fun getSaveId(): String = shortNames[Tile::class.java.simpleName].toString()

        /**
         * Creates and returns a CompoundTag containing the necessary information to spawn a tile of this type.
         *
         * @param Vector3     $pos
         * @param int|null    $face
         * @param Item|null   $item
         * @param Player|null $player
         *
         * @return CompoundTag
         */
        fun createNBT(pos: Vector3, face: Int? = null, item: Item? = null, player: Player? = null): CompoundTag {
            val nbt = CompoundTag("", [
                StringTag(Tile.TAG_ID, this.getSaveId()),
                IntTag(Tile.TAG_X, pos.x.toInt()),
                IntTag(Tile.TAG_Y, pos.y.toInt()),
                IntTag(Tile.TAG_Z, pos.z.toInt())
            ])

            this.createAdditionalNBT(nbt, pos, face, item, player)

            if (item !== null) {
                if (item.hasCustomBlockData()) {
                    item.getCustomBlockData().forEach {
                        if (it !is NamedTag) {
                            return@forEach
                        }
                        nbt.setTag(it)
                    }
                }
            }

            return nbt
        }

        /**
         * Called by createNBT() to allow descendent classes to add their own base NBT using the parameters provided.
         *
         * @param CompoundTag $nbt
         * @param Vector3     $pos
         * @param int|null    $face
         * @param Item|null   $item
         * @param Player|null $player
         */
        protected fun createAdditionalNBT(nbt: CompoundTag, pos: Vector3, face: Int? = null, item: Item? = null, player: Player? = null) {

        }
    }

    /** @var Chunk */
    var chunk: Chunk? = null
    /** @var string */
    lateinit var name: String
    /** @var int */
    var id: Int = -1
    /** @var bool */
    var closed = false
    /** @var CompoundTag */
    var namedtag: CompoundTag? = nbt
    /** @var Server */
    protected var server: Server = level.getServer()
    /** @var TimingsHandler */
    protected var timings: TimingsHandler = Timings.getTileEntityTimings(this)

    init {
        setLevel(level)
        chunk = level.getChunk(namedtag.getInt(Tile.TAG_X) shr 4, namedtag.getInt(Tile.TAG_Z) shr 4, false)
        if (chunk == null) {
            throw InvalidStateException("Cannot create tiles in unloaded chunks")
        }
        name = ""
        id = Tile.tileCount++
        this.x = namedtag.getInt(Tile.TAG_X)
        this.y = namedtag.getInt(Tile.TAG_Y)
        this.z = namedtag.getInt(Tile.TAG_Z)
        this.chunk.addTile(this)
        this.getLevel().addTile(this)
    }

    fun saveNBT() {
        namedtag.setString(Tile.TAG_ID, getSaveId())
        namedtag.setInt(Tile.TAG_X, this.x)
        namedtag.setInt(Tile.TAG_Y, this.y)
        namedtag.setInt(Tile.TAG_Z, this.z)
    }

    fun getCleanedNBT(): CompoundTag? {
        saveNBT()
        val tag = namedtag
        tag.removeTag(Tile.TAG_X, Tile.TAG_Y, Tile.TAG_Z, Tile.TAG_ID)
        return if (tag.getCount() > 0) tag else null
    }

    /**
     * @return Block
     */
    fun getBlock(): Block = level.getBlockAt(this.x, this.y, this.z)

    /**
     * @return bool
     */
    fun onUpdate() = false

    final fun scheduleUpdate() {
        level?.updateTiles?.toMutableList()!![id] = this
    }

    fun close() {
        if (!closed) {
            closed = true
            level?.updateTiles?.toMutableList()?.removeAt(id)
            if (chunk is Chunk){
                chunk.removeTile(this)
                chunk = null
            }
            if ((this.getLevel()) instanceof Level){
                this.getLevel().removeTile(this)
                setLevel(null)
            }

            namedtag = null
        }
    }
}
