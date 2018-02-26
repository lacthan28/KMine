package kmine.level

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import kmine.level.format.ChunkManager
import kmine.metadata.Metadatable
import java.io.File
import java.io.IOException
import java.lang.ref.SoftReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ThreadLocalRandom

class Level : ChunkManager, Metadatable {
    companion object {
        private val randomTickBlocks: BooleanArray = BooleanArray(256)
        init {
            randomTickBlocks[Block.GRASS] = true
            randomTickBlocks[Block.FARMLAND] = true
            randomTickBlocks[Block.MYCELIUM] = true
            randomTickBlocks[Block.SAPLING] = true
            randomTickBlocks[Block.LEAVES] = true
            randomTickBlocks[Block.LEAVES2] = true
            randomTickBlocks[Block.SNOW_LAYER] = true
            randomTickBlocks[Block.ICE] = true
            randomTickBlocks[Block.LAVA] = true
            randomTickBlocks[Block.STILL_LAVA] = true
            randomTickBlocks[Block.CACTUS] = true
            randomTickBlocks[Block.BEETROOT_BLOCK] = true
            randomTickBlocks[Block.CARROT_BLOCK] = true
            randomTickBlocks[Block.POTATO_BLOCK] = true
            randomTickBlocks[Block.MELON_STEM] = true
            randomTickBlocks[Block.PUMPKIN_STEM] = true
            randomTickBlocks[Block.WHEAT_BLOCK] = true
            randomTickBlocks[Block.SUGARCANE_BLOCK] = true
            randomTickBlocks[Block.RED_MUSHROOM] = true
            randomTickBlocks[Block.BROWN_MUSHROOM] = true
            randomTickBlocks[Block.NETHER_WART_BLOCK] = true
            randomTickBlocks[Block.FIRE] = true
            randomTickBlocks[Block.GLOWING_REDSTONE_ORE] = true
            randomTickBlocks[Block.COCOA_BLOCK] = true
        }
    }

    private var levelIdCounter = 1
    private var chunkLoaderCounter = 1
    var COMPRESSION_LEVEL = 8

    val BLOCK_UPDATE_NORMAL = 1
    val BLOCK_UPDATE_RANDOM = 2
    val BLOCK_UPDATE_SCHEDULED = 3
    val BLOCK_UPDATE_WEAK = 4
    val BLOCK_UPDATE_TOUCH = 5
    val BLOCK_UPDATE_REDSTONE = 6
    val BLOCK_UPDATE_TICK = 7

    val TIME_DAY = 0
    val TIME_SUNSET = 12000
    val TIME_NIGHT = 14000
    val TIME_SUNRISE = 23000

    val TIME_FULL = 24000

    val DIMENSION_OVERWORLD = 0
    val DIMENSION_NETHER = 1

    // Lower values use less memory
    val MAX_BLOCK_CACHE = 512

    private val blockEntities = Long2ObjectOpenHashMap()

    private val players = Long2ObjectOpenHashMap()

    private val entities = Long2ObjectOpenHashMap()

    val updateEntities: Long2ObjectOpenHashMap<Entity> = Long2ObjectOpenHashMap()

    val updateBlockEntities: Long2ObjectOpenHashMap<BlockEntity> = Long2ObjectOpenHashMap()

    private val cacheChunks = false

    private val server: Server

    private val levelId: Int

    private var provider: LevelProvider? = null

    private val loaders = Int2ObjectOpenHashMap()

    private val loaderCounter = HashMap<Int, Int>()

    private val chunkLoaders = Long2ObjectOpenHashMap()

    private val playerLoaders = Long2ObjectOpenHashMap()

    private val chunkPackets = Long2ObjectOpenHashMap()

    private val unloadQueue = Long2ObjectOpenHashMap()

    private var time: Float = 0.toFloat()
    var stopTime: Boolean = false

    var skyLightSubtracted: Float = 0.toFloat()

    private val folderName: String

    private val mutableBlock: Vector3? = null

    // Avoid OOM, gc'd references result in whole chunk being sent (possibly higher cpu)
    private val changedBlocks = Long2ObjectOpenHashMap()
    // Storing the vector is redundant
    private val changeBlocksPresent = Any()
    // Storing extra blocks past 512 is redundant
    private val changeBlocksFullMap = object : HashMap<Char, Any>() {
        override fun size(): Int {
            return Character.MAX_VALUE.toInt()
        }
    }


    private val updateQueue: BlockUpdateScheduler
//    private final TreeSet<BlockUpdateEntry> updateQueue = new TreeSet<>()
//    private final List<BlockUpdateEntry> nextTickUpdates = Lists.newArrayList()
    //private final Map<BlockVector3, Integer> updateQueueIndex = new HashMap<>()

    private val chunkSendQueue = Long2ObjectOpenHashMap()
    private val chunkSendTasks = Long2ObjectOpenHashMap()

    private val chunkPopulationQueue = Long2ObjectOpenHashMap()
    private val chunkPopulationLock = Long2ObjectOpenHashMap()
    private val chunkGenerationQueue = Long2ObjectOpenHashMap()
    private val chunkGenerationQueueSize = 8
    private val chunkPopulationQueueSize = 2

    private var autoSave = true

    private var blockMetadata: BlockMetadataStore? = null

    private var useSections: Boolean = false

    private var temporalPosition: Position? = null
    private val temporalVector: Vector3

    private val blockStates: Array<Block>

    var sleepTicks = 0

    private val chunkTickRadius: Int
    private val chunkTickList = Long2ObjectOpenHashMap()
    private val chunksPerTicks: Int
    private val clearChunksOnTick: Boolean

    protected var updateLCG = Random().nextInt()

    var timings: LevelTimings

    private var tickRate: Int = 0
    var tickRateTime = 0
    var tickRateCounter = 0

    private val generator: Class<out Generator>
    private var generatorInstance: Generator? = null

    private var raining = false
    private var rainTime = 0
    private var thundering = false
    private var thunderTime = 0

    private var levelCurrentTick: Long = 0

    private var dimension: Int = 0

    var gameRules: GameRules

    fun Level(server: Server, name: String, path: String, provider: Class<out LevelProvider>): ??? {
        this.blockStates = Block.fullList
        this.levelId = levelIdCounter++
        this.blockMetadata = BlockMetadataStore(this)
        this.server = server
        this.autoSave = server.getAutoSave()

        val convert = provider == McRegion::class.java || provider == LevelDB::class.java
        try {
            if (convert) {
                val newPath = File(path).parent + "/" + name + ".old/"
                File(path).renameTo(File(newPath))
                this.provider = provider.getConstructor(Level::class.java, String::class.java).newInstance(this, newPath)
            } else {
                this.provider = provider.getConstructor(Level::class.java, String::class.java).newInstance(this, path)
            }
        } catch (e: Exception) {
            throw LevelException("Caused by " + Utils.getExceptionMessage(e))
        }

        this.timings = LevelTimings(this)

        if (convert) {
            this.server.getLogger().info(this.server.getLanguage().translateString("nukkit.level.updating",
                    TextFormat.GREEN + this.provider!!.getName() + TextFormat.WHITE))
            val old = this.provider
            try {
                this.provider = LevelProviderConverter(this, path)
                        .from(old)
                        .to(Anvil::class.java)
                        .perform()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

            old!!.close()
        }

        this.provider!!.updateLevelName(name)

        this.server.getLogger().info(this.server.getLanguage().translateString("nukkit.level.preparing",
                TextFormat.GREEN + this.provider!!.getName() + TextFormat.WHITE))

        this.generator = Generator.getGenerator(this.provider!!.getGenerator())

        try {
            this.useSections = provider.getMethod("usesChunkSection").invoke(null) as Boolean
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        this.folderName = name
        this.time = this.provider!!.getTime()

        this.raining = this.provider!!.isRaining()
        this.rainTime = this.provider!!.getRainTime()
        if (this.rainTime <= 0) {
            setRainTime(ThreadLocalRandom.current().nextInt(168000) + 12000)
        }

        this.thundering = this.provider!!.isThundering()
        this.thunderTime = this.provider!!.getThunderTime()
        if (this.thunderTime <= 0) {
            setThunderTime(ThreadLocalRandom.current().nextInt(168000) + 12000)
        }

        this.levelCurrentTick = this.provider!!.getCurrentTick()
        this.updateQueue = BlockUpdateScheduler(this, levelCurrentTick)

        this.chunkTickRadius = Math.min(this.server.getViewDistance(),
                Math.max(1, this.server.getConfig("chunk-ticking.tick-radius", 4) as Int))
        this.chunksPerTicks = this.server.getConfig("chunk-ticking.per-tick", 40)
        this.chunkGenerationQueueSize = this.server.getConfig("chunk-generation.queue-size", 8)
        this.chunkPopulationQueueSize = this.server.getConfig("chunk-generation.population-queue-size", 2)
        this.chunkTickList.clear()
        this.clearChunksOnTick = this.server.getConfig("chunk-ticking.clear-tick-list", true)
        this.cacheChunks = this.server.getConfig("chunk-sending.cache-chunks", false)
        this.temporalPosition = Position(0, 0, 0, this)
        this.temporalVector = Vector3(0, 0, 0)
        this.tickRate = 1

        this.skyLightSubtracted = this.calculateSkylightSubtracted(1f).toFloat()
    }

    fun chunkHash(x: Int, z: Int): Long {
        return x.toLong() shl 32 or (z and 0xffffffffL)
    }

    fun blockHash(block: Vector3): BlockVector3 {
        return blockHash(block.x, block.y, block.z)
    }

    fun localBlockHash(x: Double, y: Double, z: Double): Char {
        val hi = ((x.toInt() and 15) + (z.toInt() and 15 shl 4)).toByte()
        val lo = y.toByte()
        return (hi and 0xFF shl 8 or (lo and 0xFF)).toChar()
    }

    fun getBlockXYZ(chunkHash: Long, blockHash: Char): Vector3 {
        val hi = blockHash.toInt().ushr(8).toByte().toInt()
        val lo = blockHash.toByte().toInt()
        val y = lo and 0xFF
        val x = (hi and 0xF) + (getHashX(chunkHash) shl 4)
        val z = (hi shr 4 and 0xF) + (getHashZ(chunkHash) shl 4)
        return Vector3(x, y, z)
    }

    fun blockHash(x: Double, y: Double, z: Double): BlockVector3 {
        return BlockVector3(x.toInt(), y.toInt(), z.toInt())
    }

    fun chunkBlockHash(x: Int, y: Int, z: Int): Int {
        return x shl 12 or (z shl 8) or y
    }

    fun getHashX(hash: Long): Int {
        return (hash shr 32).toInt()
    }

    fun getHashZ(hash: Long): Int {
        return hash.toInt()
    }

    fun getBlockXYZ(hash: BlockVector3): Vector3 {
        return Vector3(hash.x, hash.y, hash.z)
    }

    fun getChunkXZ(hash: Long): Chunk.Entry {
        return Chunk.Entry(getHashX(hash), getHashZ(hash))
    }

    fun generateChunkLoaderId(loader: ChunkLoader): Int {
        return if (loader.getLoaderId() == null || loader.getLoaderId() === 0) {
            chunkLoaderCounter++
        } else {
            throw IllegalStateException("ChunkLoader has a loader id already assigned: " + loader.getLoaderId())
        }
    }

    fun getTickRate(): Int {
        return tickRate
    }

    fun getTickRateTime(): Int {
        return tickRateTime
    }

    fun setTickRate(tickRate: Int) {
        this.tickRate = tickRate
    }

    fun initLevel() {
        try {
            this.generatorInstance = this.generator.getConstructor(Map<*, *>::class.java)
                    .newInstance(this.provider!!.getGeneratorOptions())
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        this.generatorInstance!!.init(this, NukkitRandom(this.getSeed()))
        this.dimension = this.generatorInstance!!.getDimension()
        this.gameRules = this.provider!!.getGamerules()


        this.registerGenerator()
    }

    fun registerGenerator() {
        val size = this.server.getScheduler().getAsyncTaskPoolSize()
        for (i in 0 until size) {
            this.server.getScheduler().scheduleAsyncTask(GeneratorRegisterTask(this, this.generatorInstance))
        }
    }

    fun unregisterGenerator() {
        val size = this.server.getScheduler().getAsyncTaskPoolSize()
        for (i in 0 until size) {
            this.server.getScheduler().scheduleAsyncTask(GeneratorUnregisterTask(this))
        }
    }

    fun getBlockMetadata(): BlockMetadataStore? {
        return this.blockMetadata
    }

    fun getServer(): Server {
        return server
    }

    fun getProvider(): LevelProvider? {
        return this.provider
    }

    fun getId(): Int {
        return this.levelId
    }

    fun close() {
        if (this.getAutoSave()) {
            this.save()
        }

        this.unregisterGenerator()

        this.provider!!.close()
        this.provider = null
        this.blockMetadata = null
        this.temporalPosition = null
        this.server.getLevels().remove(this.levelId)
    }

    fun addSound(pos: Vector3, sound: Sound) {
        this.addSound(pos, sound, 1f, 1f, *null as Array<Player>?)
    }

    fun addSound(pos: Vector3, sound: Sound, volume: Float, pitch: Float) {
        this.addSound(pos, sound, volume, pitch, *null as Array<Player>?)
    }

    fun addSound(pos: Vector3, sound: Sound, volume: Float, pitch: Float, players: Collection<Player>) {
        this.addSound(pos, sound, volume, pitch, players.stream().toArray(Player[]::new  /* Currently unsupported in Kotlin */))
    }

    fun addSound(pos: Vector3, sound: Sound, volume: Float, pitch: Float, vararg players: Player) {
        Preconditions.checkArgument(volume >= 0 && volume <= 1, "Sound volume must be between 0 and 1")
        Preconditions.checkArgument(pitch >= 0, "Sound pitch must be higher than 0")

        val packet = PlaySoundPacket()
        packet.name = sound.getSound()
        packet.volume = 1
        packet.pitch = 1
        packet.x = pos.getFloorX()
        packet.y = pos.getFloorY()
        packet.z = pos.getFloorZ()

        if (players == null || players.size == 0) {
            addChunkPacket(pos.getFloorX() shr 4, pos.getFloorZ() shr 4, packet)
        } else {
            Server.broadcastPacket(players, packet)
        }
    }

    /**
    to Broadcasts sound to players
     *
    to @param pos  position where sound should be played
    to @param type ID of the sound from cn.nukkit.network.protocol.LevelSoundEventPacket
     */
    fun addLevelSoundEvent(pos: Vector3, type: Int, pitch: Int, data: Int) {
        this.addLevelSoundEvent(pos, type, pitch, data, false)
    }

    fun addLevelSoundEvent(pos: Vector3, type: Int, pitch: Int, data: Int, isGlobal: Boolean) {
        val pk = LevelSoundEventPacket()
        pk.sound = type
        pk.pitch = pitch
        pk.extraData = data
        pk.x = pos.x
        pk.y = pos.y
        pk.z = pos.z
        pk.isGlobal = isGlobal

        this.addChunkPacket(pos.getFloorX() shr 4, pos.getFloorZ() shr 4, pk)
    }

    fun addParticle(particle: Particle) {
        this.addParticle(particle, null as Array<Player>?)
    }

    fun addParticle(particle: Particle, player: Player) {
        this.addParticle(particle, arrayOf<Player>(player))
    }

    fun addParticle(particle: Particle, players: Array<Player>?) {
        val packets = particle.encode()

        if (players == null) {
            if (packets != null) {
                for (packet in packets!!) {
                    this.addChunkPacket(particle.x as Int shr 4, particle.z as Int shr 4, packet)
                }
            }
        } else {
            if (packets != null) {
                if (packets!!.size == 1) {
                    Server.broadcastPacket(players, packets!![0])
                } else {
                    this.server.batchPackets(players, packets, false)
                }
            }
        }
    }

    fun addParticle(particle: Particle, players: Collection<Player>) {
        this.addParticle(particle, players.stream().toArray(Player[]::new  /* Currently unsupported in Kotlin */))
    }

    fun getAutoSave(): Boolean {
        return this.autoSave
    }

    fun setAutoSave(autoSave: Boolean) {
        this.autoSave = autoSave
    }

    fun unload(): Boolean {
        return this.unload(false)
    }

    fun unload(force: Boolean): Boolean {
        val ev = LevelUnloadEvent(this)

        if (this === this.server.getDefaultLevel() && !force) {
            ev.setCancelled()
        }

        this.server.getPluginManager().callEvent(ev)

        if (!force && ev.isCancelled()) {
            return false
        }

        this.server.getLogger().info(this.server.getLanguage().translateString("nukkit.level.unloading",
                TextFormat.GREEN + this.getName() + TextFormat.WHITE))
        val defaultLevel = this.server.getDefaultLevel()

        for (player in ArrayList(this.getPlayers().values)) {
            if (this === defaultLevel || defaultLevel == null) {
                player.close(player.getLeaveMessage(), "Forced default level unload")
            } else {
                player.teleport(this.server.getDefaultLevel().getSafeSpawn())
            }
        }

        if (this === defaultLevel) {
            this.server.setDefaultLevel(null)
        }

        this.close()

        return true
    }

    fun getChunkPlayers(chunkX: Int, chunkZ: Int): MutableMap<Int, Player> {
        val index = Level.chunkHash(chunkX, chunkZ)
        return if (this.playerLoaders.containsKey(index)) {
            HashMap(this.playerLoaders.get(index))
        } else {
            HashMap<K, V>()
        }
    }

    fun getChunkLoaders(chunkX: Int, chunkZ: Int): Array<ChunkLoader> {
        val index = Level.chunkHash(chunkX, chunkZ)
        return if (this.chunkLoaders.containsKey(index)) {
            this.chunkLoaders.get(index).values().stream().toArray(ChunkLoader[]::new  /* Currently unsupported in Kotlin */)
        } else {
            arrayOfNulls<ChunkLoader>(0)
        }
    }

    fun addChunkPacket(chunkX: Int, chunkZ: Int, packet: DataPacket) {
        val index = Level.chunkHash(chunkX, chunkZ)
        if (!this.chunkPackets.containsKey(index)) {
            this.chunkPackets.put(index, ArrayList<E>())
        }
        this.chunkPackets.get(index).add(packet)
    }

    fun registerChunkLoader(loader: ChunkLoader, chunkX: Int, chunkZ: Int) {
        this.registerChunkLoader(loader, chunkX, chunkZ, true)
    }

    fun registerChunkLoader(loader: ChunkLoader, chunkX: Int, chunkZ: Int, autoLoad: Boolean) {
        val hash = loader.getLoaderId()
        val index = Level.chunkHash(chunkX, chunkZ)
        if (!this.chunkLoaders.containsKey(index)) {
            this.chunkLoaders.put(index, HashMap<K, V>())
            this.playerLoaders.put(index, HashMap<K, V>())
        } else if (this.chunkLoaders.get(index).containsKey(hash)) {
            return
        }

        this.chunkLoaders.get(index).put(hash, loader)
        if (loader is Player) {
            this.playerLoaders.get(index).put(hash, loader as Player)
        }

        if (!this.loaders.containsKey(hash)) {
            this.loaderCounter[hash to 1
            this.loaders.put(hash, loader)
        } else {
            this.loaderCounter[hash to this.loaderCounter[hash] + 1
        }

        this.cancelUnloadChunkRequest(hash.toLong())

        if (autoLoad) {
            this.loadChunk(chunkX, chunkZ)
        }
    }

    fun unregisterChunkLoader(loader: ChunkLoader, chunkX: Int, chunkZ: Int) {
        val hash = loader.getLoaderId()
        val index = Level.chunkHash(chunkX, chunkZ)
        val chunkLoadersIndex = this.chunkLoaders.get(index)
        if (chunkLoadersIndex != null) {
            val oldLoader = chunkLoadersIndex!!.remove(hash)
            if (oldLoader != null) {
                if (chunkLoadersIndex!!.isEmpty()) {
                    this.chunkLoaders.remove(index)
                    this.playerLoaders.remove(index)
                    this.unloadChunkRequest(chunkX, chunkZ, true)
                } else {
                    val playerLoadersIndex = this.playerLoaders.get(index)
                    playerLoadersIndex.remove(hash)
                }

                var count = this.loaderCounter[hash]
                if (--count == 0) {
                    this.loaderCounter.remove(hash)
                    this.loaders.remove(hash)
                } else {
                    this.loaderCounter[hash to count
                }
            }
        }
    }

    fun checkTime() {
        if (!this.stopTime) {
            this.time += tickRate.toFloat()
        }
    }

    fun sendTime(vararg players: Player) {
        /*if (this.stopTime) { //TODO
            SetTimePacket pk0 = new SetTimePacket()
            pk0.time = (int) this.time
            player.dataPacket(pk0)
        }*/

        val pk = SetTimePacket()
        pk.time = this.time.toInt()

        Server.broadcastPacket(players, pk)
    }

    fun sendTime() {
        sendTime(this.players.values().stream().toArray(Player[]::new  /* Currently unsupported in Kotlin */))
    }

    fun getGameRules(): GameRules {
        return gameRules
    }

    fun doTick(currentTick: Int) {
        this.timings.doTick.startTiming()

        updateBlockLight(lightQueue)
        this.checkTime()

        // Tick Weather
        this.rainTime--
        if (this.rainTime <= 0) {
            if (!this.setRaining(!this.raining)) {
                if (this.raining) {
                    setRainTime(ThreadLocalRandom.current().nextInt(12000) + 12000)
                } else {
                    setRainTime(ThreadLocalRandom.current().nextInt(168000) + 12000)
                }
            }
        }

        this.thunderTime--
        if (this.thunderTime <= 0) {
            if (!this.setThundering(!this.thundering)) {
                if (this.thundering) {
                    setThunderTime(ThreadLocalRandom.current().nextInt(12000) + 3600)
                } else {
                    setThunderTime(ThreadLocalRandom.current().nextInt(168000) + 12000)
                }
            }
        }

        if (this.isThundering()) {
            val chunks = getChunks()
            if (chunks is Long2ObjectOpenHashMap) {
                val fastChunks = chunks as Long2ObjectOpenHashMap
                val iter = fastChunks.long2ObjectEntrySet().fastIterator()
                while (iter.hasNext()) {
                    val entry = iter.next()
                    performThunder(entry.getLongKey(), entry.getValue())
                }
            } else {
                for ((key, value) in getChunks()) {
                    performThunder(key, value)
                }
            }
        }

        this.skyLightSubtracted = this.calculateSkylightSubtracted(1f).toFloat()

        this.levelCurrentTick++

        this.unloadChunks()
        this.timings.doTickPending.startTiming()

        val polled = 0

        this.updateQueue.tick(this.getCurrentTick())
        this.timings.doTickPending.stopTiming()

        TimingsHistory.entityTicks += this.updateEntities.size()
        this.timings.entityTick.startTiming()

        if (!this.updateEntities.isEmpty()) {
            for (id in ArrayList(this.updateEntities.keySet())) {
                val entity = this.updateEntities.get(id)
                if (entity.closed || !entity.onUpdate(currentTick)) {
                    this.updateEntities.remove(id)
                }
            }
        }
        this.timings.entityTick.stopTiming()

        TimingsHistory.tileEntityTicks += this.updateBlockEntities.size()
        this.timings.blockEntityTick.startTiming()
        if (!this.updateBlockEntities.isEmpty()) {
            for (id in ArrayList(this.updateBlockEntities.keySet())) {
                if (!this.updateBlockEntities.get(id).onUpdate()) {
                    this.updateBlockEntities.remove(id)
                }
            }
        }
        this.timings.blockEntityTick.stopTiming()

        this.timings.tickChunks.startTiming()
        this.tickChunks()
        this.timings.tickChunks.stopTiming()

        if (!this.changedBlocks.isEmpty()) {
            if (!this.players.isEmpty()) {
                val iter = changedBlocks.long2ObjectEntrySet().fastIterator()
                while (iter.hasNext()) {
                    val entry = iter.next()
                    val index = entry.getKey()
                    val blocks = entry.getValue().get()
                    val chunkX = Level.getHashX(index)
                    val chunkZ = Level.getHashZ(index)
                    if (blocks == null || blocks!!.size > MAX_BLOCK_CACHE) {
                        val chunk = this.getChunk(chunkX, chunkZ)
                        for (p in this.getChunkPlayers(chunkX, chunkZ).values) {
                            p.onChunkChanged(chunk)
                        }
                    } else {
                        val toSend = this.getChunkPlayers(chunkX, chunkZ).values
                        val playerArray = toSend.toTypedArray()
                        val blocksArray = arrayOfNulls<Vector3>(blocks!!.size)
                        var i = 0
                        for (blockHash in blocks!!.keys) {
                            val hash = getBlockXYZ(index, blockHash)
                            blocksArray[i++ to hash
                        }
                        this.sendBlocks(playerArray, blocksArray, UpdateBlockPacket.FLAG_ALL)
                    }
                }
            }

            this.changedBlocks.clear()
        }

        this.processChunkRequest()

        if (this.sleepTicks > 0 && --this.sleepTicks <= 0) {
            this.checkSleep()
        }

        for (index in this.chunkPackets.keySet()) {
            val chunkX = Level.getHashX(index)
            val chunkZ = Level.getHashZ(index)
            val chunkPlayers = this.getChunkPlayers(chunkX, chunkZ).values.stream().toArray(Player[]::new  /* Currently unsupported in Kotlin */)
            if (chunkPlayers.size > 0) {
                for (pk in this.chunkPackets.get(index)) {
                    Server.broadcastPacket(chunkPlayers, pk)
                }
            }
        }

        if (gameRules.isStale()) {
            val packet = GameRulesChangedPacket()
            packet.gameRules = gameRules
            Server.broadcastPacket(players.values().toArray(arrayOfNulls<Player>(players.size())), packet)
            gameRules.refresh()
        }

        this.chunkPackets.clear()
        this.timings.doTick.stopTiming()
    }

    private fun performThunder(index: Long, chunk: FullChunk) {
        if (areNeighboringChunksLoaded(index)) return
        if (ThreadLocalRandom.current().nextInt(10000) == 0) {
            this.updateLCG = this.updateLCG to 3 + 1013904223
            val LCG = this.updateLCG shr 2

            val chunkX = chunk.getX() to 16
            val chunkZ = chunk.getZ() to 16
            val vector = this.adjustPosToNearbyEntity(Vector3(chunkX + (LCG and 15), 0, chunkZ + (LCG shr 8 and 15)))

            val bId = this.getBlockIdAt(vector.getFloorX(), vector.getFloorY(), vector.getFloorZ())
            if (bId != Block.TALL_GRASS && bId != Block.WATER)
                vector.y += 1
            val nbt = CompoundTag()
                    .putList(ListTag<DoubleTag>("Pos").add(DoubleTag("", vector.x))
                            .add(DoubleTag("", vector.y)).add(DoubleTag("", vector.z)))
                    .putList(ListTag<DoubleTag>("Motion").add(DoubleTag("", 0))
                            .add(DoubleTag("", 0)).add(DoubleTag("", 0)))
                    .putList(ListTag<FloatTag>("Rotation").add(FloatTag("", 0))
                            .add(FloatTag("", 0)))

            val bolt = EntityLightning(chunk, nbt)
            val ev = LightningStrikeEvent(this, bolt)
            getServer().getPluginManager().callEvent(ev)
            if (!ev.isCancelled()) {
                bolt.spawnToAll()
            } else {
                bolt.setEffect(false)
            }

            this.addLevelSoundEvent(vector, LevelSoundEventPacket.SOUND_THUNDER, 93, -1, false)
            this.addLevelSoundEvent(vector, LevelSoundEventPacket.SOUND_EXPLODE, 93, -1, false)
        }
    }

    fun adjustPosToNearbyEntity(pos: Vector3): Vector3 {
        var pos = pos
        pos.y = this.getHighestBlockAt(pos.getFloorX(), pos.getFloorZ())
        val axisalignedbb = SimpleAxisAlignedBB(pos.x, pos.y, pos.z, pos.getX(), 255, pos.getZ()).expand(3, 3, 3)
        val list = ArrayList<E>()

        for (entity in this.getCollidingEntities(axisalignedbb)) {
            if (entity.isAlive() && canBlockSeeSky(entity)) {
                list.add(entity)
            }
        }

        if (!list.isEmpty()) {
            return list[ThreadLocalRandom.current().nextInt(list.size)].getPosition()
        } else {
            if (pos.getY() === -1) {
                pos = pos.up(2)
            }

            return pos
        }
    }

    fun checkSleep() {
        if (this.players.isEmpty()) {
            return
        }

        var resetTime = true
        for (p in this.getPlayers().values) {
            if (!p.isSleeping()) {
                resetTime = false
                break
            }
        }

        if (resetTime) {
            val time = this.getTime() % Level.TIME_FULL

            if (time >= Level.TIME_NIGHT && time < Level.TIME_SUNRISE) {
                this.setTime(this.getTime() + Level.TIME_FULL - time)

                for (p in this.getPlayers().values) {
                    p.stopSleep()
                }
            }
        }
    }

    fun sendBlockExtraData(x: Int, y: Int, z: Int, id: Int, data: Int) {
        this.sendBlockExtraData(x, y, z, id, data, this.getChunkPlayers(x shr 4, z shr 4).values)
    }

    fun sendBlockExtraData(x: Int, y: Int, z: Int, id: Int, data: Int, players: Array<Player>) {
        val pk = LevelEventPacket()
        pk.evid = LevelEventPacket.EVENT_SET_DATA
        pk.x = x + 0.5f
        pk.y = y + 0.5f
        pk.z = z + 0.5f
        pk.data = data shl 8 or id

        Server.broadcastPacket(players, pk)
    }

    fun sendBlockExtraData(x: Int, y: Int, z: Int, id: Int, data: Int, players: Collection<Player>) {
        val pk = LevelEventPacket()
        pk.evid = LevelEventPacket.EVENT_SET_DATA
        pk.x = x + 0.5f
        pk.y = y + 0.5f
        pk.z = z + 0.5f
        pk.data = data shl 8 or id

        Server.broadcastPacket(players, pk)
    }

    fun sendBlocks(target: Array<Player>, blocks: Array<Vector3>) {
        this.sendBlocks(target, blocks, UpdateBlockPacket.FLAG_NONE)
    }

    fun sendBlocks(target: Array<Player>, blocks: Array<Vector3>, flags: Int) {
        this.sendBlocks(target, blocks, flags, false)
    }

    fun sendBlocks(target: Array<Player>, blocks: Array<Vector3>, flags: Int, optimizeRebuilds: Boolean) {
        var size = 0
        for (i in blocks.indices) {
            if (blocks[i] != null) size++
        }
        var packetIndex = 0
        val packets = arrayOfNulls<UpdateBlockPacket>(size)
        if (optimizeRebuilds) {
            val chunks = HashMap<Long, Boolean>()
            for (b in blocks) {
                if (b == null) {
                    continue
                }
                var first = false

                val index = Level.chunkHash(b!!.x as Int shr 4, b!!.z as Int shr 4)
                if (!chunks.containsKey(index)) {
                    chunks[index to true
                            first = true
                }
                val updateBlockPacket = UpdateBlockPacket()
                if (b is Block) {
                    updateBlockPacket.x = (b as Block).x
                    updateBlockPacket.y = (b as Block).y
                    updateBlockPacket.z = (b as Block).z
                    updateBlockPacket.blockId = (b as Block).getId()
                    updateBlockPacket.blockData = (b as Block).getDamage()
                    updateBlockPacket.flags = if (first) flags else UpdateBlockPacket.FLAG_NONE
                } else {
                    val fullBlock = this.getFullBlock(b!!.x as Int, b!!.y as Int, b!!.z as Int)
                    updateBlockPacket.x = b!!.x
                    updateBlockPacket.y = b!!.y
                    updateBlockPacket.z = b!!.z
                    updateBlockPacket.blockId = fullBlock shr 4
                    updateBlockPacket.blockData = fullBlock and 0xf
                    updateBlockPacket.flags = if (first) flags else UpdateBlockPacket.FLAG_NONE
                }
                packets[packetIndex++ to updateBlockPacket
            }
        } else {
            for (b in blocks) {
                if (b == null) {
                    continue
                }
                val updateBlockPacket = UpdateBlockPacket()
                if (b is Block) {
                    updateBlockPacket.x = (b as Block).x
                    updateBlockPacket.y = (b as Block).y
                    updateBlockPacket.z = (b as Block).z
                    updateBlockPacket.blockId = (b as Block).getId()
                    updateBlockPacket.blockData = (b as Block).getDamage()
                    updateBlockPacket.flags = flags
                } else {
                    val fullBlock = this.getFullBlock(b!!.x as Int, b!!.y as Int, b!!.z as Int)
                    updateBlockPacket.x = b!!.x
                    updateBlockPacket.y = b!!.y
                    updateBlockPacket.z = b!!.z
                    updateBlockPacket.blockId = fullBlock shr 4
                    updateBlockPacket.blockData = fullBlock and 0xf
                    updateBlockPacket.flags = flags
                }
                packets[packetIndex++ to updateBlockPacket
            }
        }
        this.server.batchPackets(target, packets)
    }

    private fun tickChunks() {
        if (this.chunksPerTicks <= 0 || this.loaders.isEmpty()) {
            this.chunkTickList.clear()
            return
        }

        val chunksPerLoader = Math.min(200, Math.max(1, ((this.chunksPerTicks - this.loaders.size()) as Double / this.loaders.size() + 0.5) as Int))
        var randRange = 3 + chunksPerLoader / 30
        randRange = if (randRange > this.chunkTickRadius) this.chunkTickRadius else randRange

        val random = ThreadLocalRandom.current()
        if (!this.loaders.isEmpty()) {
            for (loader in this.loaders.values()) {
                val chunkX = loader.getX() as Int shr 4
                val chunkZ = loader.getZ() as Int shr 4

                val index = Level.chunkHash(chunkX, chunkZ)
                val existingLoaders = Math.max(0, this.chunkTickList.getOrDefault(index, 0))
                this.chunkTickList.put(index, existingLoaders + 1)
                for (chunk in 0 until chunksPerLoader) {
                    val dx = random.nextInt(2 to randRange) - randRange
                    val dz = random.nextInt(2 to randRange) - randRange
                    val hash = Level.chunkHash(dx + chunkX, dz + chunkZ)
                    if (!this.chunkTickList.containsKey(hash) && provider!!.isChunkLoaded(hash)) {
                        this.chunkTickList.put(hash, -1)
                    }
                }
            }
        }

        var blockTest = 0

        if (!chunkTickList.isEmpty()) {
            val iter = chunkTickList.long2ObjectEntrySet().fastIterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                val index = entry.getLongKey()
                if (!areNeighboringChunksLoaded(index)) {
                    iter.remove()
                    continue
                }

                val loaders = entry.getValue()

                val chunkX = getHashX(index)
                val chunkZ = getHashZ(index)

                val chunk: FullChunk?
                if ((chunk = this.getChunk(chunkX, chunkZ, false)) == null) {
                    iter.remove()
                    continue
                } else if (loaders <= 0) {
                    iter.remove()
                }

                for (entity in chunk!!.getEntities().values()) {
                    entity.scheduleUpdate()
                }
                val tickSpeed = 3

                if (tickSpeed > 0) {
                    if (this.useSections) {
                        for (section in (chunk as Chunk).getSections()) {
                            if (section !is EmptyChunkSection) {
                                val Y = section.getY()
                                this.updateLCG = this.updateLCG to 3 + 1013904223
                                var k = this.updateLCG shr 2
                                var i = 0
                                while (i < tickSpeed) {
                                    val x = k and 0x0f
                                    val y = k shr 8 and 0x0f
                                    val z = k shr 16 and 0x0f

                                    val fullId = section.getFullBlock(x, y, z)
                                    val blockId = fullId shr 4
                                    if (blockId]) {
                                        val block = Block.get(fullId, this, chunkX to 16 + x, (Y shl 4) + y, chunkZ to 16 + z)
                                        block.onUpdate(BLOCK_UPDATE_RANDOM)
                                    }
                                    ++i
                                    k = k shr 10
                                }
                            }
                        }
                    } else {
                        var Y = 0
                        while (Y < 8 && (Y < 3 || blockTest != 0)) {
                            blockTest = 0
                            this.updateLCG = this.updateLCG to 3 + 1013904223
                            var k = this.updateLCG shr 2
                            var i = 0
                            while (i < tickSpeed) {
                                val x = k and 0x0f
                                val y = k shr 8 and 0x0f
                                val z = k shr 16 and 0x0f

                                val fullId = chunk!!.getFullBlock(x, y + (Y shl 4), z)
                                val blockId = fullId shr 4
                                blockTest = blockTest or fullId
                                if (this.blockId]) {
                                    val block = Block.get(fullId, this, x, y + (Y shl 4), z)
                                    block.onUpdate(BLOCK_UPDATE_RANDOM)
                                }
                                ++i
                                k = k shr 10
                            }
                            ++Y
                        }
                    }
                }
            }
        }

        if (this.clearChunksOnTick) {
            this.chunkTickList.clear()
        }
    }

    fun save(): Boolean {
        return this.save(false)
    }

    fun save(force: Boolean): Boolean {
        if (!this.getAutoSave() && !force) {
            return false
        }

        this.server.getPluginManager().callEvent(LevelSaveEvent(this))

        this.provider!!.setTime(this.time.toInt())
        this.provider!!.setRaining(this.raining)
        this.provider!!.setRainTime(this.rainTime)
        this.provider!!.setThundering(this.thundering)
        this.provider!!.setThunderTime(this.thunderTime)
        this.provider!!.setCurrentTick(this.levelCurrentTick)
        this.provider!!.setGameRules(this.gameRules)
        this.saveChunks()
        if (this.provider is BaseLevelProvider) {
            this.provider!!.saveLevelData()
        }

        return true
    }

    fun saveChunks() {
        provider!!.saveChunks()
    }

    fun updateAroundRedstone(pos: Vector3, face: BlockFace) {
        for (side in BlockFace.values()) {
            /*if(face != null && side == face) {
                continue
            }*/

            this.getBlock(pos.getSide(side))!!.onUpdate(BLOCK_UPDATE_REDSTONE)
        }
    }

    fun updateComparatorOutputLevel(v: Vector3) {
        for (face in Plane.HORIZONTAL) {
            var pos = v.getSide(face)

            if (this.isChunkLoaded(pos.x as Int shr 4, pos.z as Int shr 4)) {
                var block1 = this.getBlock(pos)

                if (BlockRedstoneDiode.isDiode(block1)) {
                    block1!!.onUpdate(BLOCK_UPDATE_REDSTONE)
                } else if (block1!!.isNormalBlock()) {
                    pos = pos.getSide(face)
                    block1 = this.getBlock(pos)

                    if (BlockRedstoneDiode.isDiode(block1)) {
                        block1!!.onUpdate(BLOCK_UPDATE_REDSTONE)
                    }
                }
            }
        }
    }

    fun updateAround(pos: Vector3) {
        updateAround(pos.x as Int, pos.y as Int, pos.z as Int)
    }

    fun updateAround(x: Int, y: Int, z: Int) {
        val ev: BlockUpdateEvent
        this.server.getPluginManager().callEvent(
                ev = BlockUpdateEvent(this.getBlock(x, y - 1, z)))
        if (!ev.isCancelled()) {
            ev.getBlock().onUpdate(BLOCK_UPDATE_NORMAL)
        }

        this.server.getPluginManager().callEvent(
                ev = BlockUpdateEvent(this.getBlock(x, y + 1, z)))
        if (!ev.isCancelled()) {
            ev.getBlock().onUpdate(BLOCK_UPDATE_NORMAL)
        }

        this.server.getPluginManager().callEvent(
                ev = BlockUpdateEvent(this.getBlock(x - 1, y, z)))
        if (!ev.isCancelled()) {
            ev.getBlock().onUpdate(BLOCK_UPDATE_NORMAL)
        }

        this.server.getPluginManager().callEvent(
                ev = BlockUpdateEvent(this.getBlock(x + 1, y, z)))
        if (!ev.isCancelled()) {
            ev.getBlock().onUpdate(BLOCK_UPDATE_NORMAL)
        }

        this.server.getPluginManager().callEvent(
                ev = BlockUpdateEvent(this.getBlock(x, y, z - 1)))
        if (!ev.isCancelled()) {
            ev.getBlock().onUpdate(BLOCK_UPDATE_NORMAL)
        }

        this.server.getPluginManager().callEvent(
                ev = BlockUpdateEvent(this.getBlock(x, y, z + 1)))
        if (!ev.isCancelled()) {
            ev.getBlock().onUpdate(BLOCK_UPDATE_NORMAL)
        }
    }

    fun scheduleUpdate(pos: Block, delay: Int) {
        this.scheduleUpdate(pos, pos, delay, 0, true)
    }

    fun scheduleUpdate(block: Block, pos: Vector3, delay: Int) {
        this.scheduleUpdate(block, pos, delay, 0, true)
    }

    fun scheduleUpdate(block: Block, pos: Vector3, delay: Int, priority: Int) {
        this.scheduleUpdate(block, pos, delay, priority, true)
    }

    fun scheduleUpdate(block: Block, pos: Vector3, delay: Int, priority: Int, checkArea: Boolean) {
        if (block.getId() === 0 || checkArea && !this.isChunkLoaded(block.getFloorX() shr 4, block.getFloorZ() shr 4)) {
            return
        }

        if (block is BlockRedstoneComparator) {
            MainLogger.getLogger().notice("schedule update: " + getCurrentTick())
        }

        val entry = BlockUpdateEntry(pos.floor(), block, delay.toLong() + getCurrentTick(), priority)

        if (!this.updateQueue.contains(entry)) {
            this.updateQueue.add(entry)
        }
    }

    fun cancelSheduledUpdate(pos: Vector3, block: Block): Boolean {
        return this.updateQueue.remove(BlockUpdateEntry(pos, block))
    }

    fun isUpdateScheduled(pos: Vector3, block: Block): Boolean {
        return this.updateQueue.contains(BlockUpdateEntry(pos, block))
    }

    fun isBlockTickPending(pos: Vector3, block: Block): Boolean {
        return this.updateQueue.isBlockTickPending(pos, block)
    }

    fun getPendingBlockUpdates(chunk: FullChunk): Set<BlockUpdateEntry> {
        val minX = (chunk.getX() shl 4) - 2
        val maxX = minX + 16 + 2
        val minZ = (chunk.getZ() shl 4) - 2
        val maxZ = minZ + 16 + 2

        return this.getPendingBlockUpdates(SimpleAxisAlignedBB(minX, 0, minZ, maxX, 256, maxZ))
    }

    fun getPendingBlockUpdates(boundingBox: AxisAlignedBB): Set<BlockUpdateEntry> {
        return updateQueue.getPendingBlockUpdates(boundingBox)
    }

    fun getCollisionBlocks(bb: AxisAlignedBB): Array<Block> {
        return this.getCollisionBlocks(bb, false)
    }

    fun getCollisionBlocks(bb: AxisAlignedBB, targetFirst: Boolean): Array<Block> {
        val minX = NukkitMath.floorDouble(bb.getMinX())
        val minY = NukkitMath.floorDouble(bb.getMinY())
        val minZ = NukkitMath.floorDouble(bb.getMinZ())
        val maxX = NukkitMath.ceilDouble(bb.getMaxX())
        val maxY = NukkitMath.ceilDouble(bb.getMaxY())
        val maxZ = NukkitMath.ceilDouble(bb.getMaxZ())

        val collides = ArrayList<E>()

        if (targetFirst) {
            for (z in minZ..maxZ) {
                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        val block = this.getBlock(this.temporalVector.setComponents(x, y, z))
                        if (block!!.getId() !== 0 && block!!.collidesWithBB(bb)) {
                            return arrayOf<Block>(block)
                        }
                    }
                }
            }
        } else {
            for (z in minZ..maxZ) {
                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        val block = this.getBlock(this.temporalVector.setComponents(x, y, z))
                        if (block!!.getId() !== 0 && block!!.collidesWithBB(bb)) {
                            collides.add(block)
                        }
                    }
                }
            }
        }

        return collides.stream().toArray(Block[]::new  /* Currently unsupported in Kotlin */)
    }

    fun isFullBlock(pos: Vector3): Boolean {
        val bb: AxisAlignedBB?
        if (pos is Block) {
            if ((pos as Block).isSolid()) {
                return true
            }
            bb = (pos as Block).getBoundingBox()
        } else {
            bb = this.getBlock(pos)!!.getBoundingBox()
        }

        return bb != null && bb!!.getAverageEdgeLength() >= 1
    }

    fun getCollisionCubes(entity: Entity, bb: AxisAlignedBB): Array<AxisAlignedBB> {
        return this.getCollisionCubes(entity, bb, true)
    }

    fun getCollisionCubes(entity: Entity, bb: AxisAlignedBB, entities: Boolean): Array<AxisAlignedBB> {
        val minX = NukkitMath.floorDouble(bb.getMinX())
        val minY = NukkitMath.floorDouble(bb.getMinY())
        val minZ = NukkitMath.floorDouble(bb.getMinZ())
        val maxX = NukkitMath.ceilDouble(bb.getMaxX())
        val maxY = NukkitMath.ceilDouble(bb.getMaxY())
        val maxZ = NukkitMath.ceilDouble(bb.getMaxZ())

        val collides = ArrayList<E>()

        for (z in minZ..maxZ) {
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    val block = this.getBlock(this.temporalVector.setComponents(x, y, z))
                    if (!block!!.canPassThrough() && block!!.collidesWithBB(bb)) {
                        collides.add(block!!.getBoundingBox())
                    }
                }
            }
        }

        if (entities) {
            for (ent in this.getCollidingEntities(bb.grow(0.25f, 0.25f, 0.25f), entity)) {
                collides.add(ent.boundingBox.clone())
            }
        }

        return collides.stream().toArray(AxisAlignedBB[]::new  /* Currently unsupported in Kotlin */)
    }

    fun hasCollision(entity: Entity, bb: AxisAlignedBB, entities: Boolean): Boolean {
        val minX = NukkitMath.floorDouble(bb.getMinX())
        val minY = NukkitMath.floorDouble(bb.getMinY())
        val minZ = NukkitMath.floorDouble(bb.getMinZ())
        val maxX = NukkitMath.ceilDouble(bb.getMaxX())
        val maxY = NukkitMath.ceilDouble(bb.getMaxY())
        val maxZ = NukkitMath.ceilDouble(bb.getMaxZ())

        for (z in minZ..maxZ) {
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    val block = this.getBlock(this.temporalVector.setComponents(x, y, z))
                    if (!block!!.canPassThrough() && block!!.collidesWithBB(bb)) {
                        return true
                    }
                }
            }
        }

        return if (entities) {
            this.getCollidingEntities(bb.grow(0.25f, 0.25f, 0.25f), entity).size > 0
        } else false
    }

    fun getFullLight(pos: Vector3): Int {
        val chunk = this.getChunk(pos.x as Int shr 4, pos.z as Int shr 4, false)
        var level = 0
        if (chunk != null) {
            level = chunk!!.getBlockSkyLight(pos.x as Int and 0x0f, pos.y as Int and 0xff, pos.z as Int and 0x0f)
            level -= this.skyLightSubtracted.toInt()

            if (level < 15) {
                level = Math.max(chunk!!.getBlockLight(pos.x as Int and 0x0f, pos.y as Int and 0xff, pos.z as Int and 0x0f),
                        level)
            }
        }

        return level
    }

    fun calculateSkylightSubtracted(tickDiff: Float): Int {
        val angle = this.calculateCelestialAngle(getTime(), tickDiff)
        var light = 1 - (MathHelper.cos(angle to (Math.PI.toFloat() to 2f)) to 2 + 0.5f)
        light = if (light < 0) 0 else if (light > 1) 1 else light
        light = 1 - light
        light = (light.toDouble() to ((if (isRaining()) 1 else 0) - 5f.toDouble() / 16.0)).toFloat()
        light = (light.toDouble() to ((if (isThundering()) 1 else 0) - 5f.toDouble() / 16.0)).toFloat()
        light = 1 - light
        return (light to 11f).toInt()
    }

    fun calculateCelestialAngle(time: Int, tickDiff: Float): Float {
        var angle = (time.toFloat() + tickDiff) / 24000f - 0.25f

        if (angle < 0) {
            ++angle
        }

        if (angle > 1) {
            --angle
        }

        val i = 1 - ((Math.cos(angle.toDouble() to Math.PI) + 1) / 2.0).toFloat()
        angle = angle + (i - angle) / 3
        return angle
    }

    fun getMoonPhase(worldTime: Long): Int {
        return (worldTime / 24000 % 8 + 8).toInt() % 8
    }

    fun getFullBlock(x: Int, y: Int, z: Int): Int {
        return this.getChunk(x shr 4, z shr 4, false)!!.getFullBlock(x and 0x0f, y and 0xff, z and 0x0f)
    }

    fun getBlock(pos: Vector3): Block? {
        return this.getBlock(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ())
    }

    fun getBlock(x: Int, y: Int, z: Int): Block {
        val fullState: Int
        if (y >= 0 && y < 256) {
            val cx = x shr 4
            val cz = z shr 4
            val chunk = getChunk(cx, cz)
            if (chunk != null) {
                fullState = chunk!!.getFullBlock(x and 0xF, y, z and 0xF)
            } else {
                fullState = 0
            }
        } else {
            fullState = 0
        }
        val block = this.blockStates[fullState and 0xFFF].clone()
        block.x = x
        block.y = y
        block.z = z
        block.level = this
        return block
    }

    fun updateAllLight(pos: Vector3) {
        this.updateBlockSkyLight(pos.x as Int, pos.y as Int, pos.z as Int)
        this.addLightUpdate(pos.x as Int, pos.y as Int, pos.z as Int)
    }

    fun updateBlockSkyLight(x: Int, y: Int, z: Int) {
        // todo
    }

    fun updateBlockLight(map: MutableMap<Long, Map<Char, Any>>) {
        var size = map.size
        if (size == 0) {
            return
        }
        val lightPropagationQueue = ConcurrentLinkedQueue<E>()
        val lightRemovalQueue = ConcurrentLinkedQueue<Array<Any>>()
        val visited = ConcurrentHashMap<K, V>(8, 0.9f, 1)
        val removalVisited = ConcurrentHashMap<K, V>(8, 0.9f, 1)

        val iter = map.entries.iterator()
        while (iter.hasNext() && size-- > 0) {
            val entry = iter.next()
            iter.remove()
            val index = entry.key
            val blocks = entry.value
            val chunkX = Level.getHashX(index)
            val chunkZ = Level.getHashZ(index)
            val bx = chunkX shl 4
            val bz = chunkZ shl 4
            for (blockHash in blocks.keys) {
                val hi = blockHash.toInt().ushr(8).toByte().toInt()
                val lo = blockHash.toByte().toInt()
                val y = lo and 0xFF
                val x = (hi and 0xF) + bx
                val z = (hi shr 4 and 0xF) + bz
                val chunk = getChunk(x shr 4, z shr 4, false)
                if (chunk != null) {
                    val lcx = x and 0xF
                    val lcz = z and 0xF
                    val oldLevel = chunk!!.getBlockLight(lcx, y, lcz)
                    val newLevel = Block.light[chunk!!.getBlockId(lcx, y, lcz)]
                    if (oldLevel != newLevel) {
                        this.setBlockLightAt(x, y, z, newLevel)
                        if (newLevel < oldLevel) {
                            removalVisited[Level.blockHash(x.toDouble(), y.toDouble(), z.toDouble()) to changeBlocksPresent
                                    lightRemovalQueue . add (arrayOf(BlockVector3(x, y, z), oldLevel))
                        } else {
                            visited[Level.blockHash(x.toDouble(), y.toDouble(), z.toDouble()) to changeBlocksPresent
                                    lightPropagationQueue . add (BlockVector3(x, y, z))
                        }
                    }
                }
            }
        }

        while (!lightRemovalQueue.isEmpty()) {
            val `val` = lightRemovalQueue.poll()
            val node = `val`[0] as BlockVector3
            val lightLevel = `val`[1] as Int

            this.computeRemoveBlockLight(node.x as Int - 1, node.y as Int, node.z as Int, lightLevel, lightRemovalQueue,
                    lightPropagationQueue, removalVisited, visited)
            this.computeRemoveBlockLight(node.x as Int + 1, node.y as Int, node.z as Int, lightLevel, lightRemovalQueue,
                    lightPropagationQueue, removalVisited, visited)
            this.computeRemoveBlockLight(node.x as Int, node.y as Int - 1, node.z as Int, lightLevel, lightRemovalQueue,
                    lightPropagationQueue, removalVisited, visited)
            this.computeRemoveBlockLight(node.x as Int, node.y as Int + 1, node.z as Int, lightLevel, lightRemovalQueue,
                    lightPropagationQueue, removalVisited, visited)
            this.computeRemoveBlockLight(node.x as Int, node.y as Int, node.z as Int - 1, lightLevel, lightRemovalQueue,
                    lightPropagationQueue, removalVisited, visited)
            this.computeRemoveBlockLight(node.x as Int, node.y as Int, node.z as Int + 1, lightLevel, lightRemovalQueue,
                    lightPropagationQueue, removalVisited, visited)
        }

        while (!lightPropagationQueue.isEmpty()) {
            val node = lightPropagationQueue.poll()
            val lightLevel = this.getBlockLightAt(node.x as Int, node.y as Int, node.z as Int) - Block.lightFilter[this.getBlockIdAt(node.x as Int, node.y as Int, node.z as Int)]

            if (lightLevel >= 1) {
                this.computeSpreadBlockLight(node.x as Int - 1, node.y as Int, node.z as Int, lightLevel,
                        lightPropagationQueue, visited)
                this.computeSpreadBlockLight(node.x as Int + 1, node.y as Int, node.z as Int, lightLevel,
                        lightPropagationQueue, visited)
                this.computeSpreadBlockLight(node.x as Int, node.y as Int - 1, node.z as Int, lightLevel,
                        lightPropagationQueue, visited)
                this.computeSpreadBlockLight(node.x as Int, node.y as Int + 1, node.z as Int, lightLevel,
                        lightPropagationQueue, visited)
                this.computeSpreadBlockLight(node.x as Int, node.y as Int, node.z as Int - 1, lightLevel,
                        lightPropagationQueue, visited)
                this.computeSpreadBlockLight(node.x, node.y as Int, node.z as Int + 1, lightLevel,
                        lightPropagationQueue, visited)
            }
        }
    }

    private fun computeRemoveBlockLight(x: Int, y: Int, z: Int, currentLight: Int, queue: Queue<Array<Any>>,
                                        spreadQueue: Queue<BlockVector3>, visited: MutableMap<BlockVector3, Any>, spreadVisited: MutableMap<BlockVector3, Any>) {
        val current = this.getBlockLightAt(x, y, z)
        val index = Level.blockHash(x.toDouble(), y.toDouble(), z.toDouble())
        if (current != 0 && current < currentLight) {
            this.setBlockLightAt(x, y, z, 0)
            if (current > 1) {
                if (!visited.containsKey(index)) {
                    visited[index to changeBlocksPresent
                            queue . add (arrayOf(BlockVector3(x, y, z), current))
                }
            }
        } else if (current >= currentLight) {
            if (!spreadVisited.containsKey(index)) {
                spreadVisited[index to changeBlocksPresent
                        spreadQueue . add (BlockVector3(x, y, z))
            }
        }
    }

    private fun computeSpreadBlockLight(x: Int, y: Int, z: Int, currentLight: Int, queue: Queue<BlockVector3>,
                                        visited: MutableMap<BlockVector3, Any>) {
        val current = this.getBlockLightAt(x, y, z)
        val index = Level.blockHash(x.toDouble(), y.toDouble(), z.toDouble())

        if (current < currentLight - 1) {
            this.setBlockLightAt(x, y, z, currentLight)

            if (!visited.containsKey(index)) {
                visited[index to changeBlocksPresent
                if (currentLight > 1) {
                    queue.add(BlockVector3(x, y, z))
                }
            }
        }
    }

    private val lightQueue = ConcurrentHashMap<Long, Map<Char, Any>>(8, 0.9f, 1)

    fun addLightUpdate(x: Int, y: Int, z: Int) {
        val index = chunkHash(x shr 4, z shr 4)
        var currentMap: MutableMap<Char, Any>? = lightQueue[index]
        if (currentMap == null) {
            currentMap = ConcurrentHashMap(8, 0.9f, 1)
            this.lightQueue[index to currentMap
        }
        currentMap[Level.localBlockHash(x.toDouble(), y.toDouble(), z.toDouble()) to changeBlocksPresent
    }

    fun setBlock(pos: Vector3, block: Block): Boolean {
        return this.setBlock(pos, block, false)
    }

    fun setBlock(pos: Vector3, block: Block, direct: Boolean): Boolean {
        return this.setBlock(pos, block, direct, true)
    }

    fun setBlock(pos: Vector3, block: Block, direct: Boolean, update: Boolean): Boolean {
        return setBlock(pos.x as Int, pos.y as Int, pos.z as Int, block, direct, update)
    }

    fun setBlock(x: Int, y: Int, z: Int, block: Block, direct: Boolean, update: Boolean): Boolean {
        var block = block
        if (y < 0 || y >= 256) {
            return false
        }
        val chunk = this.getChunk(x shr 4, z shr 4, true)
        val blockPrevious: Block
        //        synchronized (chunk) {
        blockPrevious = chunk!!.getAndSetBlock(x and 0xF, y, z and 0xF, block)
        if (blockPrevious.getFullId() === block.getFullId()) {
            return false
        }
        //        }
        block.x = x
        block.y = y
        block.z = z
        block.level = this
        val cx = x shr 4
        val cz = z shr 4
        val index = Level.chunkHash(cx, cz)
        if (direct) {
            this.sendBlocks(this.getChunkPlayers(cx, cz).values.stream().toArray(Player[]::new  /* Currently unsupported in Kotlin */), arrayOf<Block>(block), UpdateBlockPacket.FLAG_ALL_PRIORITY)
        } else {
            addBlockChange(index, x, y, z)
        }

        for (loader in this.getChunkLoaders(cx, cz)) {
            loader.onBlockChanged(block)
        }
        if (update) {
            if (blockPrevious.isTransparent() !== block.isTransparent() || blockPrevious.getLightLevel() !== block.getLightLevel()) {
                addLightUpdate(x, y, z)
            }
            val ev = BlockUpdateEvent(block)
            this.server.getPluginManager().callEvent(ev)
            if (!ev.isCancelled()) {
                for (entity in this.getNearbyEntities(SimpleAxisAlignedBB(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1))) {
                    entity.scheduleUpdate()
                }
                block = ev.getBlock()
                block.onUpdate(BLOCK_UPDATE_NORMAL)
                this.updateAround(x, y, z)
            }
        }
        return true
    }

    private fun addBlockChange(x: Int, y: Int, z: Int) {
        val index = Level.chunkHash(x shr 4, z shr 4)
        addBlockChange(index, x, y, z)
    }

    private fun addBlockChange(index: Long, x: Int, y: Int, z: Int) {
        val current = changedBlocks.computeIfAbsent(index, { k to SoftReference(HashMap<Any, Any>()) })
        val currentMap = current.get()
        if (currentMap !== changeBlocksFullMap && currentMap != null) {
            if (currentMap!!.size > MAX_BLOCK_CACHE) {
                this.changedBlocks.put(index, SoftReference(changeBlocksFullMap))
            } else {
                currentMap!!.put(Level.localBlockHash(x.toDouble(), y.toDouble(), z.toDouble()), changeBlocksPresent)
            }
        }
    }

    fun dropItem(source: Vector3?, item: Item) {
        this.dropItem(source, item, null)
    }

    fun dropItem(source: Vector3?, item: Item, motion: Vector3?) {
        this.dropItem(source, item, motion, 10)
    }

    fun dropItem(source: Vector3?, item: Item, motion: Vector3?, delay: Int) {
        this.dropItem(source, item, motion, false, delay)
    }

    fun dropItem(source: Vector3?, item: Item, motion: Vector3?, dropAround: Boolean, delay: Int) {
        var motion = motion
        if (motion == null) {
            if (dropAround) {
                val f = ThreadLocalRandom.current().nextFloat() to 0.5f
                val f1 = ThreadLocalRandom.current().nextFloat() to (Math.PI.toFloat() to 2)

                motion = Vector3(-MathHelper.sin(f1) to f, 0.20000000298023224, MathHelper.cos(f1) to f)
            } else {
                motion = Vector3(java.util.Random().nextDouble() to 0.2 - 0.1, 0.2,
                        java.util.Random().nextDouble() to 0.2 - 0.1)
            }
        }

        val itemTag = NBTIO.putItemHelper(item)
        itemTag.setName("Item")

        if (item.getId() > 0 && item.getCount() > 0) {
            val itemEntity = EntityItem(
                    this.getChunk(source!!.getX() as Int shr 4, source!!.getZ() as Int shr 4, true),
                    CompoundTag().putList(ListTag<DoubleTag>("Pos").add(DoubleTag("", source!!.getX()))
                            .add(DoubleTag("", source!!.getY())).add(DoubleTag("", source!!.getZ())))

                            .putList(ListTag<DoubleTag>("Motion").add(DoubleTag("", motion!!.x))
                                    .add(DoubleTag("", motion!!.y)).add(DoubleTag("", motion!!.z)))

                            .putList(ListTag<FloatTag>("Rotation")
                                    .add(FloatTag("", java.util.Random().nextFloat() to 360))
                                    .add(FloatTag("", 0)))

                            .putShort("Health", 5).putCompound("Item", itemTag).putShort("PickupDelay", delay))

            itemEntity.spawnToAll()
        }
    }

    fun useBreakOn(vector: Vector3): Item? {
        return this.useBreakOn(vector, null)
    }

    fun useBreakOn(vector: Vector3, item: Item?): Item? {
        return this.useBreakOn(vector, item, null)
    }

    fun useBreakOn(vector: Vector3, item: Item?, player: Player?): Item? {
        return this.useBreakOn(vector, item, player, false)
    }

    fun useBreakOn(vector: Vector3, item: Item?, player: Player?, createParticles: Boolean): Item? {
        var item = item
        if (player != null && player!!.getGamemode() > 1) {
            return null
        }
        val target = this.getBlock(vector)
        val drops: Array<Item>
        if (item == null) {
            item = ItemBlock(BlockAir(), 0, 0)
        }

        if (player != null) {
            var breakTime = target!!.getBreakTime(item, player)
            // this in
            // block
            // class

            if (player!!.isCreative() && breakTime > 0.15) {
                breakTime = 0.15
            }

            if (player!!.hasEffect(Effect.SWIFTNESS)) {
                breakTime *= 1 - 0.2 to (player!!.getEffect(Effect.SWIFTNESS).getAmplifier() + 1)
            }

            if (player!!.hasEffect(Effect.MINING_FATIGUE)) {
                breakTime *= 1 - 0.3 to (player!!.getEffect(Effect.MINING_FATIGUE).getAmplifier() + 1)
            }

            val eff = item!!.getEnchantment(Enchantment.ID_EFFICIENCY)

            if (eff != null && eff!!.getLevel() > 0) {
                breakTime *= 1 - 0.3 to eff!!.getLevel()
            }

            breakTime -= 0.15

            val ev = BlockBreakEvent(player, target, item, player!!.isCreative(),
                    player!!.lastBreak + breakTime to 1000 > System.currentTimeMillis())

            val distance: Double
            if (player!!.isSurvival() && !target!!.isBreakable(item)) {
                ev.setCancelled()
            } else if (!player!!.isOp() && (distance = this.server.getSpawnRadius()) > -1) {
                val t = Vector2(target!!.x, target!!.z)
                val s = Vector2(this.getSpawnLocation().x, this.getSpawnLocation().z)
                if (!this.server.getOps().getAll().isEmpty() && t.distance(s) <= distance) {
                    ev.setCancelled()
                }
            }

            this.server.getPluginManager().callEvent(ev)
            if (ev.isCancelled()) {
                return null
            }

            if (!ev.getInstaBreak() && ev.isFastBreak()) {
                return null
            }

            player!!.lastBreak = System.currentTimeMillis()

            drops = ev.getDrops()
        } else if (!target!!.isBreakable(item)) {
            return null
        } else {
            drops = target!!.getDrops(item)
        }

        val above = this.getBlock(Vector3(target!!.x, target!!.y + 1, target!!.z))
        if (above != null) {
            if (above!!.getId() === Item.FIRE) {
                this.setBlock(above, BlockAir(), true)
            }
        }

        val tag = item!!.getNamedTagEntry("CanDestroy")
        if (tag is ListTag) {
            var canBreak = false
            for (v in (tag as ListTag<Tag>).getAll()) {
                if (v is StringTag) {
                    val entry = Item.fromString((v as StringTag).data)
                    if (entry.getId() > 0 && entry.getBlock() != null && entry.getBlock().getId() === target!!.getId()) {
                        canBreak = true
                        break
                    }
                }
            }

            if (!canBreak) {
                return null
            }
        }

        if (createParticles) {
            val players = this.getChunkPlayers(target!!.x as Int shr 4, target!!.z as Int shr 4)

            this.addParticle(DestroyBlockParticle(target!!.add(0.5), target), players.values)

            if (player != null) {
                players.remove(player!!.getLoaderId())
            }
        }

        target!!.onBreak(item)

        val blockEntity = this.getBlockEntity(target)
        if (blockEntity != null) {
            if (blockEntity is InventoryHolder) {
                if (blockEntity is BlockEntityChest) {
                    (blockEntity as BlockEntityChest).unpair()
                }

                for (chestItem in (blockEntity as InventoryHolder).getInventory().getContents().values()) {
                    this.dropItem(target, chestItem)
                }
            }

            blockEntity!!.close()

            this.updateComparatorOutputLevel(target)
        }

        item!!.useOn(target)
        if (item!!.isTool() && item!!.getDamage() >= item!!.getMaxDurability()) {
            item = ItemBlock(BlockAir(), 0, 0)
        }

        if (this.gameRules.getBoolean(GameRule.DO_TILE_DROPS)) {
            val dropExp = target!!.getDropExp()
            if (player != null) {
                player!!.addExperience(dropExp)
                if (player!!.isSurvival()) {
                    for (ii in 1..dropExp) {
                        this.dropExpOrb(target, 1)
                    }
                }
            }

            if (player == null || player!!.isSurvival()) {
                for (drop in drops) {
                    if (drop.getCount() > 0) {
                        this.dropItem(vector.add(0.5, 0.5, 0.5), drop)
                    }
                }
            }
        }

        return item
    }

    fun dropExpOrb(source: Vector3, exp: Int) {
        dropExpOrb(source, exp, null)
    }

    fun dropExpOrb(source: Vector3, exp: Int, motion: Vector3?) {
        dropExpOrb(source, exp, motion, 10)
    }

    fun dropExpOrb(source: Vector3, exp: Int, motion: Vector3?, delay: Int) {
        var motion = motion
        motion = if (motion == null)
            Vector3(java.util.Random().nextDouble() to 0.2 - 0.1, 0.2,
                    java.util.Random().nextDouble() to 0.2 - 0.1)
        else
            motion
        val nbt = CompoundTag()
                .putList(ListTag<DoubleTag>("Pos").add(DoubleTag("", source.getX()))
                        .add(DoubleTag("", source.getY())).add(DoubleTag("", source.getZ())))
                .putList(ListTag<DoubleTag>("Motion").add(DoubleTag("", motion!!.getX()))
                        .add(DoubleTag("", motion!!.getY())).add(DoubleTag("", motion!!.getZ())))
                .putList(ListTag<FloatTag>("Rotation").add(FloatTag("", 0)).add(FloatTag("", 0)))
        val entity = EntityXPOrb(this.getChunk(source.getFloorX() shr 4, source.getFloorZ() shr 4), nbt)
        val xpOrb = entity as EntityXPOrb
        xpOrb.setExp(exp)
        xpOrb.setPickupDelay(delay)
        xpOrb.saveNBT()

        xpOrb.spawnToAll()

    }

    fun useItemOn(vector: Vector3, item: Item, face: BlockFace, fx: Float, fy: Float, fz: Float): Item? {
        return this.useItemOn(vector, item, face, fx, fy, fz, null)
    }

    fun useItemOn(vector: Vector3, item: Item, face: BlockFace, fx: Float, fy: Float, fz: Float, player: Player?): Item? {
        return this.useItemOn(vector, item, face, fx, fy, fz, player, false)
    }


    fun useItemOn(vector: Vector3, item: Item, face: BlockFace, fx: Float, fy: Float, fz: Float, player: Player?, playSound: Boolean): Item? {
        var item = item
        val target = this.getBlock(vector)
        var block = target!!.getSide(face)

        if (block.y > 255 || block.y < 0) {
            return null
        }

        if (target!!.getId() === Item.AIR) {
            return null
        }

        if (player != null) {
            val ev = PlayerInteractEvent(player, item, target, face,
                    if (target!!.getId() === 0) Action.RIGHT_CLICK_AIR else Action.RIGHT_CLICK_BLOCK)

            if (player!!.getGamemode() > 2) {
                ev.setCancelled()
            }

            val distance = this.server.getSpawnRadius()
            if (!player!!.isOp() && distance > -1) {
                val t = Vector2(target!!.x, target!!.z)
                val s = Vector2(this.getSpawnLocation().x, this.getSpawnLocation().z)
                if (!this.server.getOps().getAll().isEmpty() && t.distance(s) <= distance) {
                    ev.setCancelled()
                }
            }

            this.server.getPluginManager().callEvent(ev)
            if (!ev.isCancelled()) {
                target!!.onUpdate(BLOCK_UPDATE_TOUCH)
                if ((!player!!.isSneaking() || player!!.getInventory().getItemInHand().isNull()) && target!!.canBeActivated() && target!!.onActivate(item, player)) {
                    return item
                }

                if (item.canBeActivated() && item.onActivate(this, player, block, target, face, fx, fy, fz)) {
                    if (item.getCount() <= 0) {
                        item = ItemBlock(BlockAir(), 0, 0)
                        return item
                    }
                }
            } else {
                return null
            }

        } else if (target!!.canBeActivated() && target!!.onActivate(item, null)) {
            return item
        }
        val hand: Block
        if (item.canBePlaced()) {
            hand = item.getBlock()
            hand.position(block)
        } else {
            return null
        }

        if (!(block.canBeReplaced() || hand.getId() === Item.SLAB && block.getId() === Item.SLAB)) {
            return null
        }

        if (target!!.canBeReplaced()) {
            block = target
            hand.position(block)
        }

        if (!hand.canPassThrough() && hand.getBoundingBox() != null) {
            val entities = this.getCollidingEntities(hand.getBoundingBox())
            var realCount = 0
            for (e in entities) {
                if (e is EntityArrow || e is EntityItem || e is Player && (e as Player).isSpectator()) {
                    continue
                }
                ++realCount
            }

            if (player != null) {
                val diff = player!!.getNextPosition().subtract(player!!.getPosition())
                if (diff.lengthSquared() > 0.00001) {
                    val bb = player!!.getBoundingBox().getOffsetBoundingBox(diff.x, diff.y, diff.z)
                    if (hand.getBoundingBox().intersectsWith(bb)) {
                        ++realCount
                    }
                }
            }

            if (realCount > 0) {
                return null // Entity in block
            }
        }

        val tag = item.getNamedTagEntry("CanPlaceOn")
        if (tag is ListTag) {
            var canPlace = false
            for (v in (tag as ListTag<Tag>).getAll()) {
                if (v is StringTag) {
                    val entry = Item.fromString((v as StringTag).data)
                    if (entry.getId() > 0 && entry.getBlock() != null && entry.getBlock().getId() === target!!.getId()) {
                        canPlace = true
                        break
                    }
                }
            }

            if (!canPlace) {
                return null
            }
        }

        if (player != null) {
            val event = BlockPlaceEvent(player, hand, block, target, item)
            val distance = this.server.getSpawnRadius()
            if (!player!!.isOp() && distance > -1) {
                val t = Vector2(target!!.x, target!!.z)
                val s = Vector2(this.getSpawnLocation().x, this.getSpawnLocation().z)
                if (!this.server.getOps().getAll().isEmpty() && t.distance(s) <= distance) {
                    event.setCancelled()
                }
            }

            this.server.getPluginManager().callEvent(event)
            if (event.isCancelled()) {
                return null
            }
        }

        if (!hand.place(item, block, target, face, fx, fy, fz, player)) {
            return null
        }

        if (player != null) {
            if (!player!!.isCreative()) {
                item.setCount(item.getCount() - 1)
            }
        }

        if (playSound) {
            this.addLevelSoundEvent(hand, LevelSoundEventPacket.SOUND_PLACE, 1, item.getId(), false)
        }

        if (item.getCount() <= 0) {
            item = ItemBlock(BlockAir(), 0, 0)
        }
        return item
    }

    fun getEntity(entityId: Long): Entity? {
        return if (this.entities.containsKey(entityId)) this.entities.get(entityId) else null
    }

    fun getEntities(): Array<Entity> {
        return entities.values().stream().toArray(Entity[]::new  /* Currently unsupported in Kotlin */)
    }

    fun getCollidingEntities(bb: AxisAlignedBB): Array<Entity> {
        return this.getCollidingEntities(bb, null)
    }

    fun getCollidingEntities(bb: AxisAlignedBB, entity: Entity?): Array<Entity> {
        val nearby = ArrayList<E>()

        if (entity == null || entity!!.canCollide()) {
            val minX = NukkitMath.floorDouble((bb.getMinX() - 2) / 16)
            val maxX = NukkitMath.ceilDouble((bb.getMaxX() + 2) / 16)
            val minZ = NukkitMath.floorDouble((bb.getMinZ() - 2) / 16)
            val maxZ = NukkitMath.ceilDouble((bb.getMaxZ() + 2) / 16)

            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    for (ent in this.getChunkEntities(x, z).values) {
                        if ((entity == null || ent !== entity && entity!!.canCollideWith(ent)) && ent.boundingBox.intersectsWith(bb)) {
                            nearby.add(ent)
                        }
                    }
                }
            }
        }

        return nearby.stream().toArray(Entity[]::new  /* Currently unsupported in Kotlin */)
    }

    fun getNearbyEntities(bb: AxisAlignedBB): Array<Entity> {
        return this.getNearbyEntities(bb, null)
    }

    private val EMPTY_ENTITY_ARR = arrayOfNulls<Entity>(0)
    private val ENTITY_BUFFER = arrayOfNulls<Entity>(512)

    fun getNearbyEntities(bb: AxisAlignedBB, entity: Entity?): Array<Entity> {
        var index = 0

        val minX = NukkitMath.floorDouble((bb.getMinX() - 2) to 0.0625)
        val maxX = NukkitMath.ceilDouble((bb.getMaxX() + 2) to 0.0625)
        val minZ = NukkitMath.floorDouble((bb.getMinZ() - 2) to 0.0625)
        val maxZ = NukkitMath.ceilDouble((bb.getMaxZ() + 2) to 0.0625)

        var overflow: ArrayList<Entity>? = null

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (ent in this.getChunkEntities(x, z).values) {
                    if (ent !== entity && ent.boundingBox.intersectsWith(bb)) {
                        if (index < ENTITY_BUFFER.size) {
                            ENTITY_BUFFER[index to ent
                        } else {
                            if (overflow == null) overflow = ArrayList<E>(1024)
                            overflow.add(ent)
                        }
                        index++
                    }
                }
            }
        }

        if (index == 0) return EMPTY_ENTITY_ARR
        val copy: Array<Entity>
        if (overflow == null) {
            copy = Arrays.copyOfRange(ENTITY_BUFFER, 0, index)
            Arrays.fill(ENTITY_BUFFER, 0, index, null)
        } else {
            copy = arrayOfNulls<Entity>(ENTITY_BUFFER.size + overflow.size)
            System.arraycopy(ENTITY_BUFFER, 0, copy, 0, ENTITY_BUFFER.size)
            for (i in overflow.indices) {
                copy[ENTITY_BUFFER.size + i to overflow[i]
            }
        }
        return copy
    }

    fun getBlockEntities(): Map<Long, BlockEntity> {
        return blockEntities
    }

    fun getBlockEntityById(blockEntityId: Long): BlockEntity? {
        return if (this.blockEntities.containsKey(blockEntityId)) this.blockEntities.get(blockEntityId) else null
    }

    fun getPlayers(): Map<Long, Player> {
        return players
    }

    fun getLoaders(): Map<Int, ChunkLoader> {
        return loaders
    }

    fun getBlockEntity(pos: Vector3): BlockEntity? {
        val chunk = this.getChunk(pos.x as Int shr 4, pos.z as Int shr 4, false)

        return if (chunk != null) {
            chunk!!.getTile(pos.x as Int and 0x0f, pos.y as Int and 0xff, pos.z as Int and 0x0f)
        } else null

    }

    fun getChunkEntities(X: Int, Z: Int): Map<Long, Entity> {
        val chunk: FullChunk?
        return if ((chunk = this.getChunk(X, Z)) != null) chunk!!.getEntities() else emptyMap<K, V>()
    }

    fun getChunkBlockEntities(X: Int, Z: Int): Map<Long, BlockEntity> {
        val chunk: FullChunk?
        return if ((chunk = this.getChunk(X, Z)) != null) chunk!!.getBlockEntities() else emptyMap<K, V>()
    }

    fun getBlockIdAt(x: Int, y: Int, z: Int): Int {
        return this.getChunk(x shr 4, z shr 4, true)!!.getBlockId(x and 0x0f, y and 0xff, z and 0x0f)
    }

    fun setBlockIdAt(x: Int, y: Int, z: Int, id: Int) {
        this.getChunk(x shr 4, z shr 4, true)!!.setBlockId(x and 0x0f, y and 0xff, z and 0x0f, id and 0xff)
        addBlockChange(x, y, z)
        temporalVector.setComponents(x, y, z)
        for (loader in this.getChunkLoaders(x shr 4, z shr 4)) {
            loader.onBlockChanged(temporalVector)
        }
    }

    fun getBlockExtraDataAt(x: Int, y: Int, z: Int): Int {
        return this.getChunk(x shr 4, z shr 4, true)!!.getBlockExtraData(x and 0x0f, y and 0xff, z and 0x0f)
    }

    fun setBlockExtraDataAt(x: Int, y: Int, z: Int, id: Int, data: Int) {
        this.getChunk(x shr 4, z shr 4, true)!!.setBlockExtraData(x and 0x0f, y and 0xff, z and 0x0f, data shl 8 or id)

        this.sendBlockExtraData(x, y, z, id, data)
    }

    fun getBlockDataAt(x: Int, y: Int, z: Int): Int {
        return this.getChunk(x shr 4, z shr 4, true)!!.getBlockData(x and 0x0f, y and 0xff, z and 0x0f)
    }

    fun setBlockDataAt(x: Int, y: Int, z: Int, data: Int) {
        this.getChunk(x shr 4, z shr 4, true)!!.setBlockData(x and 0x0f, y and 0xff, z and 0x0f, data and 0x0f)
        addBlockChange(x, y, z)
        temporalVector.setComponents(x, y, z)
        for (loader in this.getChunkLoaders(x shr 4, z shr 4)) {
            loader.onBlockChanged(temporalVector)
        }
    }

    fun getBlockSkyLightAt(x: Int, y: Int, z: Int): Int {
        return this.getChunk(x shr 4, z shr 4, true)!!.getBlockSkyLight(x and 0x0f, y and 0xff, z and 0x0f)
    }

    fun setBlockSkyLightAt(x: Int, y: Int, z: Int, level: Int) {
        this.getChunk(x shr 4, z shr 4, true)!!.setBlockSkyLight(x and 0x0f, y and 0xff, z and 0x0f, level and 0x0f)
    }

    fun getBlockLightAt(x: Int, y: Int, z: Int): Int {
        return this.getChunk(x shr 4, z shr 4, true)!!.getBlockLight(x and 0x0f, y and 0xff, z and 0x0f)
    }

    fun setBlockLightAt(x: Int, y: Int, z: Int, level: Int) {
        this.getChunk(x shr 4, z shr 4, true)!!.setBlockLight(x and 0x0f, y and 0xff, z and 0x0f, level and 0x0f)
    }

    fun getBiomeId(x: Int, z: Int): Int {
        return this.getChunk(x shr 4, z shr 4, true)!!.getBiomeId(x and 0x0f, z and 0x0f)
    }

    fun setBiomeId(x: Int, z: Int, biomeId: Int) {
        this.getChunk(x shr 4, z shr 4, true)!!.setBiomeId(x and 0x0f, z and 0x0f, biomeId and 0x0f)
    }

    fun getHeightMap(x: Int, z: Int): Int {
        return this.getChunk(x shr 4, z shr 4, true)!!.getHeightMap(x and 0x0f, z and 0x0f)
    }

    fun setHeightMap(x: Int, z: Int, value: Int) {
        this.getChunk(x shr 4, z shr 4, true)!!.setHeightMap(x and 0x0f, z and 0x0f, value and 0x0f)
    }

    fun getBiomeColor(x: Int, z: Int): IntArray {
        return this.getChunk(x shr 4, z shr 4, true)!!.getBiomeColor(x and 0x0f, z and 0x0f)
    }

    fun setBiomeColor(x: Int, z: Int, R: Int, G: Int, B: Int) {
        this.getChunk(x shr 4, z shr 4, true)!!.setBiomeColor(x and 0x0f, z and 0x0f, R, G, B)
    }

    fun getChunks(): Map<Long, FullChunk> {
        return provider!!.getLoadedChunks()
    }

    fun getChunk(chunkX: Int, chunkZ: Int): BaseFullChunk? {
        return this.getChunk(chunkX, chunkZ, false)
    }

    fun getChunk(chunkX: Int, chunkZ: Int, create: Boolean): BaseFullChunk? {
        val index = Level.chunkHash(chunkX, chunkZ)
        var chunk = this.provider!!.getLoadedChunk(index)
        if (chunk == null) {
            chunk = this.forceLoadChunk(index, chunkX, chunkZ, create)
        }
        return chunk
    }

    fun generateChunkCallback(x: Int, z: Int, chunk: BaseFullChunk?) {
        var chunk = chunk
        Timings.generationCallbackTimer.startTiming()
        val index = Level.chunkHash(x, z)
        if (this.chunkPopulationQueue.containsKey(index)) {
            val oldChunk = this.getChunk(x, z, false)
            for (xx in -1..1) {
                for (zz in -1..1) {
                    this.chunkPopulationLock.remove(Level.chunkHash(x + xx, z + zz))
                }
            }
            this.chunkPopulationQueue.remove(index)
            chunk!!.setProvider(this.provider)
            this.setChunk(x, z, chunk, false)
            chunk = this.getChunk(x, z, false)
            if (chunk != null && (oldChunk == null || !oldChunk!!.isPopulated()) && chunk!!.isPopulated()
                    && chunk!!.getProvider() != null) {
                this.server.getPluginManager().callEvent(ChunkPopulateEvent(chunk))

                for (loader in this.getChunkLoaders(x, z)) {
                    loader.onChunkPopulated(chunk)
                }
            }
        } else if (this.chunkGenerationQueue.containsKey(index) || this.chunkPopulationLock.containsKey(index)) {
            this.chunkGenerationQueue.remove(index)
            this.chunkPopulationLock.remove(index)
            chunk!!.setProvider(this.provider)
            this.setChunk(x, z, chunk, false)
        } else {
            chunk!!.setProvider(this.provider)
            this.setChunk(x, z, chunk, false)
        }
        Timings.generationCallbackTimer.stopTiming()
    }

    fun setChunk(chunkX: Int, chunkZ: Int) {
        this.setChunk(chunkX, chunkZ, null)
    }

    fun setChunk(chunkX: Int, chunkZ: Int, chunk: BaseFullChunk?) {
        this.setChunk(chunkX, chunkZ, chunk, true)
    }

    fun setChunk(chunkX: Int, chunkZ: Int, chunk: BaseFullChunk?, unload: Boolean) {
        if (chunk == null) {
            return
        }

        val index = Level.chunkHash(chunkX, chunkZ)
        val oldChunk = this.getChunk(chunkX, chunkZ, false)
        if (unload && oldChunk != null) {
            this.unloadChunk(chunkX, chunkZ, false, false)

            this.provider!!.setChunk(chunkX, chunkZ, chunk)
        } else {
            val oldEntities = if (oldChunk != null) oldChunk!!.getEntities() else emptyMap<K, V>()

            val oldBlockEntities = if (oldChunk != null) oldChunk!!.getBlockEntities() else emptyMap<K, V>()

            if (!oldEntities.isEmpty()) {
                val iter = oldEntities.entries.iterator()
                while (iter.hasNext()) {
                    val entry = iter.next()
                    val entity = entry.value
                    chunk!!.addEntity(entity)
                    if (oldChunk != null) {
                        iter.remove()
                        oldChunk!!.removeEntity(entity)
                        entity.chunk = chunk
                    }
                }
            }

            if (!oldBlockEntities.isEmpty()) {
                val iter = oldBlockEntities.entries.iterator()
                while (iter.hasNext()) {
                    val entry = iter.next()
                    val blockEntity = entry.value
                    chunk!!.addBlockEntity(blockEntity)
                    if (oldChunk != null) {
                        iter.remove()
                        oldChunk!!.removeBlockEntity(blockEntity)
                        blockEntity.chunk = chunk
                    }
                }
            }

            this.provider!!.setChunk(chunkX, chunkZ, chunk)
        }

        chunk!!.setChanged()

        if (!this.isChunkInUse(index)) {
            this.unloadChunkRequest(chunkX, chunkZ)
        } else {
            for (loader in this.getChunkLoaders(chunkX, chunkZ)) {
                loader.onChunkChanged(chunk)
            }
        }
    }

    fun getHighestBlockAt(x: Int, z: Int): Int {
        return this.getChunk(x shr 4, z shr 4, true)!!.getHighestBlockAt(x and 0x0f, z and 0x0f)
    }

    fun getMapColorAt(x: Int, z: Int): BlockColor {
        var y = getHighestBlockAt(x, z)
        while (y > 1) {
            val block = getBlock(Vector3(x, y, z))
            val blockColor = block!!.getColor()
            if (blockColor.getAlpha() === 0x00) {
                y--
            } else {
                return blockColor
            }
        }
        return BlockColor.VOID_BLOCK_COLOR
    }

    fun isChunkLoaded(x: Int, z: Int): Boolean {
        return this.provider!!.isChunkLoaded(x, z)
    }

    private fun areNeighboringChunksLoaded(hash: Long): Boolean {
        return this.provider!!.isChunkLoaded(hash + 1) &&
                this.provider!!.isChunkLoaded(hash - 1) &&
                this.provider!!.isChunkLoaded(hash + (1L shl 32)) &&
                this.provider!!.isChunkLoaded(hash - (1L shl 32))
    }

    fun isChunkGenerated(x: Int, z: Int): Boolean {
        val chunk = this.getChunk(x, z)
        return chunk != null && chunk!!.isGenerated()
    }

    fun isChunkPopulated(x: Int, z: Int): Boolean {
        val chunk = this.getChunk(x, z)
        return chunk != null && chunk!!.isPopulated()
    }

    fun getSpawnLocation(): Position {
        return Position.fromObject(this.provider!!.getSpawn(), this)
    }

    fun setSpawnLocation(pos: Vector3) {
        val previousSpawn = this.getSpawnLocation()
        this.provider!!.setSpawn(pos)
        this.server.getPluginManager().callEvent(SpawnChangeEvent(this, previousSpawn))
        val pk = SetSpawnPositionPacket()
        pk.spawnType = SetSpawnPositionPacket.TYPE_WORLD_SPAWN
        pk.x = pos.getFloorX()
        pk.y = pos.getFloorY()
        pk.z = pos.getFloorZ()
        for (p in getPlayers().values) p.dataPacket(pk)
    }

    fun requestChunk(x: Int, z: Int, player: Player) {
        val index = Level.chunkHash(x, z)


        if (!this.chunkSendQueue.containsKey(index)) {
            this.chunkSendQueue.put(index, HashMap<K, V>())
        }

        this.chunkSendQueue.get(index).put(player.getLoaderId(), player)
    }

    private fun sendChunk(x: Int, z: Int, index: Long, packet: DataPacket) {
        if (this.chunkSendTasks.containsKey(index)) {
            for (player in this.chunkSendQueue.get(index).values()) {
                if (player.isConnected() && player.usedChunks.containsKey(index)) {
                    player.sendChunk(x, z, packet)
                }
            }

            this.chunkSendQueue.remove(index)
            this.chunkSendTasks.remove(index)
        }
    }

    private fun processChunkRequest() {
        this.timings.syncChunkSendTimer.startTiming()
        for (index in ImmutableList.copyOf(this.chunkSendQueue.keySet())) {
            if (this.chunkSendTasks.containsKey(index)) {
                continue
            }
            val x = getHashX(index!!)
            val z = getHashZ(index!!)
            this.chunkSendTasks.put(index, java.lang.Boolean.TRUE)
            val chunk = getChunk(x, z)
            if (chunk != null) {
                val packet = chunk!!.getChunkPacket()
                if (packet != null) {
                    this.sendChunk(x, z, index!!, packet)
                    continue
                }
            }
            this.timings.syncChunkSendPrepareTimer.startTiming()
            val task = this.provider!!.requestChunkTask(x, z)
            if (task != null) {
                this.server.getScheduler().scheduleAsyncTask(task)
            }
            this.timings.syncChunkSendPrepareTimer.stopTiming()
        }
        this.timings.syncChunkSendTimer.stopTiming()
    }

    fun chunkRequestCallback(timestamp: Long, x: Int, z: Int, payload: ByteArray) {
        this.timings.syncChunkSendTimer.startTiming()
        val index = Level.chunkHash(x, z)

        if (this.cacheChunks) {
            val data = Player.getChunkCacheFromData(x, z, payload)
            val chunk = getChunk(x, z, false)
            if (chunk != null && chunk!!.getChanges() <= timestamp) {
                chunk!!.setChunkPacket(data)
            }
            this.sendChunk(x, z, index, data)
            this.timings.syncChunkSendTimer.stopTiming()
            return
        }

        if (this.chunkSendTasks.containsKey(index)) {
            for (player in this.chunkSendQueue.get(index).values()) {
                if (player.isConnected() && player.usedChunks.containsKey(index)) {
                    player.sendChunk(x, z, payload)
                }
            }

            this.chunkSendQueue.remove(index)
            this.chunkSendTasks.remove(index)
        }
        this.timings.syncChunkSendTimer.stopTiming()
    }

    fun removeEntity(entity: Entity) {
        if (entity.getLevel() !== this) {
            throw LevelException("Invalid Entity level")
        }

        if (entity is Player) {
            this.players.remove(entity.getId())
            this.checkSleep()
        } else {
            entity.close()
        }

        this.entities.remove(entity.getId())
        this.updateEntities.remove(entity.getId())
    }

    fun addEntity(entity: Entity) {
        if (entity.getLevel() !== this) {
            throw LevelException("Invalid Entity level")
        }

        if (entity is Player) {
            this.players.put(entity.getId(), entity as Player)
        }
        this.entities.put(entity.getId(), entity)
    }

    fun addBlockEntity(blockEntity: BlockEntity) {
        if (blockEntity.getLevel() !== this) {
            throw LevelException("Invalid Block Entity level")
        }
        blockEntities.put(blockEntity.getId(), blockEntity)
    }

    fun removeBlockEntity(blockEntity: BlockEntity) {
        if (blockEntity.getLevel() !== this) {
            throw LevelException("Invalid Block Entity level")
        }
        blockEntities.remove(blockEntity.getId())
        updateBlockEntities.remove(blockEntity.getId())
    }

    fun isChunkInUse(x: Int, z: Int): Boolean {
        return isChunkInUse(Level.chunkHash(x, z))
    }

    fun isChunkInUse(hash: Long): Boolean {
        return this.chunkLoaders.containsKey(hash) && !this.chunkLoaders.get(hash).isEmpty()
    }

    fun loadChunk(x: Int, z: Int): Boolean {
        return this.loadChunk(x, z, true)
    }

    fun loadChunk(x: Int, z: Int, generate: Boolean): Boolean {
        val index = Level.chunkHash(x, z)
        return if (this.provider!!.isChunkLoaded(index)) {
            true
        } else forceLoadChunk(index, x, z, generate) != null
    }

    private fun forceLoadChunk(index: Long, x: Int, z: Int, generate: Boolean): BaseFullChunk? {
        this.timings.syncChunkLoadTimer.startTiming()
        val chunk = this.provider!!.getChunk(x, z, generate)
        if (chunk == null) {
            if (generate) {
                throw IllegalStateException("Could not create new Chunk")
            }
            this.timings.syncChunkLoadTimer.stopTiming()
            return chunk
        }

        if (chunk!!.getProvider() != null) {
            this.server.getPluginManager().callEvent(ChunkLoadEvent(chunk, !chunk!!.isGenerated()))
        } else {
            this.unloadChunk(x, z, false)
            this.timings.syncChunkLoadTimer.stopTiming()
            return chunk
        }

        chunk!!.initChunk()

        if (!chunk!!.isLightPopulated() && chunk!!.isPopulated()
                && this.getServer().getConfig("chunk-ticking.light-updates", false)) {
            this.getServer().getScheduler().scheduleAsyncTask(LightPopulationTask(this, chunk))
        }

        if (this.isChunkInUse(index)) {
            this.unloadQueue.remove(index)
            for (loader in this.getChunkLoaders(x, z)) {
                loader.onChunkLoaded(chunk)
            }
        } else {
            this.unloadQueue.put(index, System.currentTimeMillis())
        }
        this.timings.syncChunkLoadTimer.stopTiming()
        return chunk
    }

    private fun queueUnloadChunk(x: Int, z: Int) {
        val index = Level.chunkHash(x, z)
        this.unloadQueue.put(index, System.currentTimeMillis())
    }

    fun unloadChunkRequest(x: Int, z: Int): Boolean {
        return this.unloadChunkRequest(x, z, true)
    }

    fun unloadChunkRequest(x: Int, z: Int, safe: Boolean): Boolean {
        if (safe && this.isChunkInUse(x, z) || this.isSpawnChunk(x, z)) {
            return false
        }

        this.queueUnloadChunk(x, z)

        return true
    }

    fun cancelUnloadChunkRequest(x: Int, z: Int) {
        this.cancelUnloadChunkRequest(Level.chunkHash(x, z))
    }

    fun cancelUnloadChunkRequest(hash: Long) {
        this.unloadQueue.remove(hash)
    }

    fun unloadChunk(x: Int, z: Int): Boolean {
        return this.unloadChunk(x, z, true)
    }

    fun unloadChunk(x: Int, z: Int, safe: Boolean): Boolean {
        return this.unloadChunk(x, z, safe, true)
    }

    fun unloadChunk(x: Int, z: Int, safe: Boolean, trySave: Boolean): Boolean {
        if (safe && this.isChunkInUse(x, z)) {
            return false
        }

        if (!this.isChunkLoaded(x, z)) {
            return true
        }

        this.timings.doChunkUnload.startTiming()

        val index = Level.chunkHash(x, z)

        val chunk = this.getChunk(x, z)

        if (chunk != null && chunk!!.getProvider() != null) {
            val ev = ChunkUnloadEvent(chunk)
            this.server.getPluginManager().callEvent(ev)
            if (ev.isCancelled()) {
                this.timings.doChunkUnload.stopTiming()
                return false
            }
        }

        try {
            if (chunk != null) {
                if (trySave && this.getAutoSave()) {
                    var entities = 0
                    for (e in chunk!!.getEntities().values()) {
                        if (e is Player) {
                            continue
                        }
                        ++entities
                    }

                    if (chunk!!.hasChanged() || !chunk!!.getBlockEntities().isEmpty() || entities > 0) {
                        this.provider!!.setChunk(x, z, chunk)
                        this.provider!!.saveChunk(x, z)
                    }
                }
                for (loader in this.getChunkLoaders(x, z)) {
                    loader.onChunkUnloaded(chunk)
                }
            }
            this.provider!!.unloadChunk(x, z, safe)
        } catch (e: Exception) {
            val logger = this.server.getLogger()
            logger.error(this.server.getLanguage().translateString("nukkit.level.chunkUnloadError", e.toString()))
            logger.logException(e)
        }

        this.timings.doChunkUnload.stopTiming()

        return true
    }

    fun isSpawnChunk(X: Int, Z: Int): Boolean {
        val spawn = this.provider!!.getSpawn()
        return Math.abs(X - (spawn.getFloorX() shr 4)) <= 1 && Math.abs(Z - (spawn.getFloorZ() shr 4)) <= 1
    }

    fun getSafeSpawn(): Position? {
        return this.getSafeSpawn(null)
    }

    fun getSafeSpawn(spawn: Vector3?): Position? {
        var spawn = spawn
        if (spawn == null || spawn!!.y < 1) {
            spawn = this.getSpawnLocation()
        }

        if (spawn != null) {
            val v = spawn!!.floor()
            val chunk = this.getChunk(v.x as Int shr 4, v.z as Int shr 4, false)
            val x = v.x as Int and 0x0f
            val z = v.z as Int and 0x0f
            if (chunk != null) {
                var y = Math.min(254, v.y)
                var wasAir = chunk!!.getBlockId(x, y - 1, z) === 0
                while (y > 0) {
                    val b = chunk!!.getFullBlock(x, y, z)
                    val block = Block.get(b shr 4, b and 0x0f)
                    if (this.isFullBlock(block)) {
                        if (wasAir) {
                            y++
                            break
                        }
                    } else {
                        wasAir = true
                    }
                    --y
                }

                while (y >= 0 && y < 256) {
                    var b = chunk!!.getFullBlock(x, y + 1, z)
                    var block = Block.get(b shr 4, b and 0x0f)
                    if (!this.isFullBlock(block)) {
                        b = chunk!!.getFullBlock(x, y, z)
                        block = Block.get(b shr 4, b and 0x0f)
                        if (!this.isFullBlock(block)) {
                            return Position(spawn!!.x, if (y == spawn!!.y as Int) spawn!!.y else y, spawn!!.z, this)
                        }
                    } else {
                        ++y
                    }
                    ++y
                }

                v.y = y
            }

            return Position(spawn!!.x, v.y, spawn!!.z, this)
        }

        return null
    }

    fun getTime(): Int {
        return time.toInt()
    }

    fun isDaytime(): Boolean {
        return this.skyLightSubtracted < 4
    }

    fun getCurrentTick(): Long {
        return this.levelCurrentTick
    }

    fun getName(): String {
        return this.provider!!.getName()
    }

    fun getFolderName(): String {
        return this.folderName
    }

    fun setTime(time: Int) {
        this.time = time.toFloat()
        this.sendTime()
    }

    fun stopTime() {
        this.stopTime = true
        this.sendTime()
    }

    fun startTime() {
        this.stopTime = false
        this.sendTime()
    }

    fun getSeed(): Long {
        return this.provider!!.getSeed()
    }

    fun setSeed(seed: Int) {
        this.provider!!.setSeed(seed)
    }

    fun populateChunk(x: Int, z: Int): Boolean {
        return this.populateChunk(x, z, false)
    }

    fun populateChunk(x: Int, z: Int, force: Boolean): Boolean {
        val index = Level.chunkHash(x, z)
        if (this.chunkPopulationQueue.containsKey(index) || this.chunkPopulationQueue.size() >= this.chunkPopulationQueueSize && !force) {
            return false
        }

        val chunk = this.getChunk(x, z, true)
        var populate: Boolean
        if (!chunk!!.isPopulated()) {
            Timings.populationTimer.startTiming()
            populate = true
            for (xx in -1..1) {
                for (zz in -1..1) {
                    if (this.chunkPopulationLock.containsKey(Level.chunkHash(x + xx, z + zz))) {

                        populate = false
                        break
                    }
                }
            }

            if (populate) {
                if (!this.chunkPopulationQueue.containsKey(index)) {
                    this.chunkPopulationQueue.put(index, java.lang.Boolean.TRUE)
                    for (xx in -1..1) {
                        for (zz in -1..1) {
                            this.chunkPopulationLock.put(Level.chunkHash(x + xx, z + zz), java.lang.Boolean.TRUE)
                        }
                    }

                    val task = PopulationTask(this, chunk)
                    this.server.getScheduler().scheduleAsyncTask(task)
                }
            }
            Timings.populationTimer.stopTiming()
            return false
        }

        return true
    }

    fun generateChunk(x: Int, z: Int) {
        this.generateChunk(x, z, false)
    }

    fun generateChunk(x: Int, z: Int, force: Boolean) {
        if (this.chunkGenerationQueue.size() >= this.chunkGenerationQueueSize && !force) {
            return
        }

        val index = Level.chunkHash(x, z)
        if (!this.chunkGenerationQueue.containsKey(index)) {
            Timings.generationTimer.startTiming()
            this.chunkGenerationQueue.put(index, java.lang.Boolean.TRUE)
            val task = GenerationTask(this, this.getChunk(x, z, true))
            this.server.getScheduler().scheduleAsyncTask(task)
            Timings.generationTimer.stopTiming()
        }
    }

    fun regenerateChunk(x: Int, z: Int) {
        this.unloadChunk(x, z, false)

        this.cancelUnloadChunkRequest(x, z)

        this.generateChunk(x, z)
    }

    fun doChunkGarbageCollection() {
        this.timings.doChunkGC.startTiming()
        // remove all invaild block entities.
        val toClose = ArrayList<E>()
        if (!blockEntities.isEmpty()) {
            val iter = blockEntities.entrySet().iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                val aBlockEntity = entry.value
                if (aBlockEntity != null) {
                    if (!aBlockEntity!!.isValid()) {
                        iter.remove()
                        aBlockEntity!!.close()
                    }
                } else {
                    iter.remove()
                }
            }
        }

        for (entry in provider!!.getLoadedChunks().entrySet()) {
            val index = entry.key
            if (!this.unloadQueue.containsKey(index)) {
                val chunk = entry.value
                val X = chunk.getX()
                val Z = chunk.getZ()
                if (!this.isSpawnChunk(X, Z)) {
                    this.unloadChunkRequest(X, Z, true)
                }
            }
        }

        this.provider!!.doGarbageCollection()
        this.timings.doChunkGC.stopTiming()
    }

    fun doGarbageCollection(allocatedTime: Long) {
        var allocatedTime = allocatedTime
        val start = System.currentTimeMillis()
        if (unloadChunks(start, allocatedTime, false)) {
            allocatedTime = allocatedTime - (System.currentTimeMillis() - start)
            provider!!.doGarbageCollection(allocatedTime)
        }
    }

    fun unloadChunks() {
        this.unloadChunks(false)
    }

    fun unloadChunks(force: Boolean) {
        unloadChunks(96, force)
    }

    fun unloadChunks(maxUnload: Int, force: Boolean) {
        var maxUnload = maxUnload
        if (!this.unloadQueue.isEmpty()) {
            val now = System.currentTimeMillis()

            var toRemove: PrimitiveList? = null
            val iter = unloadQueue.long2ObjectEntrySet().fastIterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                val index = entry.getLongKey()

                if (isChunkInUse(index)) {
                    continue
                }

                if (!force) {
                    val time = entry.getValue()
                    if (maxUnload <= 0) {
                        break
                    } else if (time > now - 30000) {
                        continue
                    }
                }

                if (toRemove == null) toRemove = PrimitiveList(Long::class.javaPrimitiveType)
                toRemove!!.add(index)
            }

            if (toRemove != null) {
                val size = toRemove!!.size()
                for (i in 0 until size) {
                    val index = toRemove!!.getLong(i)
                    val X = getHashX(index)
                    val Z = getHashZ(index)

                    if (this.unloadChunk(X, Z, true)) {
                        this.unloadQueue.remove(index)
                        --maxUnload
                    }
                }
            }
        }
    }

    private var lastUnloadIndex: Int = 0

    /**
    to @param now
    to @param allocatedTime
    to @param force
    to @return true if there is allocated time remaining
     */
    private fun unloadChunks(now: Long, allocatedTime: Long, force: Boolean): Boolean {
        if (!this.unloadQueue.isEmpty()) {
            var result = true
            val maxIterations = this.unloadQueue.size()

            if (lastUnloadIndex > maxIterations) lastUnloadIndex = 0
            var iter = this.unloadQueue.long2ObjectEntrySet().fastIterator()
            if (lastUnloadIndex != 0) iter.skip(lastUnloadIndex)

            var toUnload: PrimitiveList? = null

            for (i in 0 until maxIterations) {
                if (!iter.hasNext()) {
                    iter = this.unloadQueue.long2ObjectEntrySet().fastIterator()
                }
                val entry = iter.next()

                val index = entry.getLongKey()

                if (isChunkInUse(index)) {
                    continue
                }

                if (!force) {
                    val time = entry.getValue()
                    if (time > now - 30000) {
                        continue
                    }
                }
                if (toUnload == null) toUnload = PrimitiveList(Long::class.javaPrimitiveType)
                toUnload!!.add(index)
            }

            if (toUnload != null) {
                val arr = toUnload!!.getArray() as LongArray
                for (index in arr) {
                    val X = getHashX(index)
                    val Z = getHashZ(index)
                    if (this.unloadChunk(X, Z, true)) {
                        this.unloadQueue.remove(index)
                        if (System.currentTimeMillis() - now >= allocatedTime) {
                            result = false
                            break
                        }
                    }
                }
            }
            return result
        } else {
            return true
        }
    }

    @Throws(Exception::class)
    fun setMetadata(metadataKey: String, newMetadataValue: MetadataValue) {
        this.server.getLevelMetadata().setMetadata(this, metadataKey, newMetadataValue)
    }

    @Throws(Exception::class)
    fun getMetadata(metadataKey: String): List<MetadataValue> {
        return this.server.getLevelMetadata().getMetadata(this, metadataKey)
    }

    @Throws(Exception::class)
    fun hasMetadata(metadataKey: String): Boolean {
        return this.server.getLevelMetadata().hasMetadata(this, metadataKey)
    }

    @Throws(Exception::class)
    fun removeMetadata(metadataKey: String, owningPlugin: Plugin) {
        this.server.getLevelMetadata().removeMetadata(this, metadataKey, owningPlugin)
    }

    fun addEntityMotion(chunkX: Int, chunkZ: Int, entityId: Long, x: Double, y: Double, z: Double) {
        val pk = SetEntityMotionPacket()
        pk.eid = entityId
        pk.motionX = x.toFloat()
        pk.motionY = y.toFloat()
        pk.motionZ = z.toFloat()

        this.addChunkPacket(chunkX, chunkZ, pk)
    }

    fun addEntityMovement(chunkX: Int, chunkZ: Int, entityId: Long, x: Double, y: Double, z: Double, yaw: Double, pitch: Double, headYaw: Double) {
        val pk = MoveEntityPacket()
        pk.eid = entityId
        pk.x = x.toFloat()
        pk.y = y.toFloat()
        pk.z = z.toFloat()
        pk.yaw = yaw.toFloat()
        pk.headYaw = yaw.toFloat()
        pk.pitch = pitch.toFloat()

        this.addChunkPacket(chunkX, chunkZ, pk)
    }

    fun isRaining(): Boolean {
        return this.raining
    }

    fun setRaining(raining: Boolean): Boolean {
        val ev = WeatherChangeEvent(this, raining)
        this.getServer().getPluginManager().callEvent(ev)

        if (ev.isCancelled()) {
            return false
        }

        this.raining = raining

        val pk = LevelEventPacket()
        // These numbers are from Minecraft

        if (raining) {
            pk.evid = LevelEventPacket.EVENT_START_RAIN
            pk.data = ThreadLocalRandom.current().nextInt(50000) + 10000
            setRainTime(ThreadLocalRandom.current().nextInt(12000) + 12000)
        } else {
            pk.evid = LevelEventPacket.EVENT_STOP_RAIN
            setRainTime(ThreadLocalRandom.current().nextInt(168000) + 12000)
        }

        Server.broadcastPacket(this.getPlayers().values, pk)

        return true
    }

    fun getRainTime(): Int {
        return this.rainTime
    }

    fun setRainTime(rainTime: Int) {
        this.rainTime = rainTime
    }

    fun isThundering(): Boolean {
        return isRaining() && this.thundering
    }

    fun setThundering(thundering: Boolean): Boolean {
        val ev = ThunderChangeEvent(this, thundering)
        this.getServer().getPluginManager().callEvent(ev)

        if (ev.isCancelled()) {
            return false
        }

        if (thundering && !isRaining()) {
            setRaining(true)
        }

        this.thundering = thundering

        val pk = LevelEventPacket()
        // These numbers are from Minecraft
        if (thundering) {
            pk.evid = LevelEventPacket.EVENT_START_THUNDER
            pk.data = ThreadLocalRandom.current().nextInt(50000) + 10000
            setThunderTime(ThreadLocalRandom.current().nextInt(12000) + 3600)
        } else {
            pk.evid = LevelEventPacket.EVENT_STOP_THUNDER
            setThunderTime(ThreadLocalRandom.current().nextInt(168000) + 12000)
        }

        Server.broadcastPacket(this.getPlayers().values, pk)

        return true
    }

    fun getThunderTime(): Int {
        return this.thunderTime
    }

    fun setThunderTime(thunderTime: Int) {
        this.thunderTime = thunderTime
    }

    fun sendWeather(players: Array<Player>?) {
        var players = players
        if (players == null) {
            players = this.getPlayers().values.stream().toArray(Player[]::new  /* Currently unsupported in Kotlin */)
        }

        val pk = LevelEventPacket()

        if (this.isRaining()) {
            pk.evid = LevelEventPacket.EVENT_START_RAIN
            pk.data = ThreadLocalRandom.current().nextInt(50000) + 10000
        } else {
            pk.evid = LevelEventPacket.EVENT_STOP_RAIN
        }

        Server.broadcastPacket(players, pk)

        if (this.isThundering()) {
            pk.evid = LevelEventPacket.EVENT_START_THUNDER
            pk.data = ThreadLocalRandom.current().nextInt(50000) + 10000
        } else {
            pk.evid = LevelEventPacket.EVENT_STOP_THUNDER
        }

        Server.broadcastPacket(players, pk)
    }

    fun sendWeather(player: Player?) {
        if (player != null) {
            this.sendWeather(arrayOf<Player>(player))
        }
    }

    fun sendWeather(players: Collection<Player>?) {
        var players = players
        if (players == null) {
            players = this.getPlayers().values
        }
        this.sendWeather(players.stream().toArray(Player[]::new  /* Currently unsupported in Kotlin */))
    }

    fun getDimension(): Int {
        return dimension
    }

    fun canBlockSeeSky(pos: Vector3): Boolean {
        return this.getHighestBlockAt(pos.getFloorX(), pos.getFloorZ()) < pos.getY()
    }

    fun getStrongPower(pos: Vector3, direction: BlockFace): Int {
        return this.getBlock(pos)!!.getStrongPower(direction)
    }

    fun getStrongPower(pos: Vector3): Int {
        var i = 0
        i = Math.max(i, this.getStrongPower(pos.down(), BlockFace.DOWN))

        if (i >= 15) {
            return i
        } else {
            i = Math.max(i, this.getStrongPower(pos.up(), BlockFace.UP))

            if (i >= 15) {
                return i
            } else {
                i = Math.max(i, this.getStrongPower(pos.north(), BlockFace.NORTH))

                if (i >= 15) {
                    return i
                } else {
                    i = Math.max(i, this.getStrongPower(pos.south(), BlockFace.SOUTH))

                    if (i >= 15) {
                        return i
                    } else {
                        i = Math.max(i, this.getStrongPower(pos.west(), BlockFace.WEST))

                        if (i >= 15) {
                            return i
                        } else {
                            i = Math.max(i, this.getStrongPower(pos.east(), BlockFace.EAST))
                            return if (i >= 15) i else i
                        }
                    }
                }
            }
        }
    }

    fun isSidePowered(pos: Vector3, face: BlockFace): Boolean {
        return this.getRedstonePower(pos, face) > 0
    }

    fun getRedstonePower(pos: Vector3, face: BlockFace): Int {
        val block = this.getBlock(pos)
        return if (block!!.isNormalBlock()) this.getStrongPower(pos) else block!!.getWeakPower(face)
    }

    fun isBlockPowered(pos: Vector3): Boolean {
        return this.getRedstonePower(pos.north(), BlockFace.NORTH) > 0 || this.getRedstonePower(pos.south(), BlockFace.SOUTH) > 0 || this.getRedstonePower(pos.west(), BlockFace.WEST) > 0 || this.getRedstonePower(pos.east(), BlockFace.EAST) > 0 || this.getRedstonePower(pos.down(), BlockFace.DOWN) > 0 || this.getRedstonePower(pos.up(), BlockFace.UP) > 0
    }

    fun isBlockIndirectlyGettingPowered(pos: Vector3): Int {
        var power = 0

        for (face in BlockFace.values()) {
            val blockPower = this.getRedstonePower(pos.getSide(face), face)

            if (blockPower >= 15) {
                return 15
            }

            if (blockPower > power) {
                power = blockPower
            }
        }

        return power
    }

    fun isAreaLoaded(bb: AxisAlignedBB): Boolean {
        if (bb.getMaxY() < 0 || bb.getMinY() >= 256) {
            return false
        }
        val minX = NukkitMath.floorDouble(bb.getMinX()) shr 4
        val minZ = NukkitMath.floorDouble(bb.getMinZ()) shr 4
        val maxX = NukkitMath.floorDouble(bb.getMaxX()) shr 4
        val maxZ = NukkitMath.floorDouble(bb.getMaxZ()) shr 4

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                if (!this.isChunkLoaded(x, z)) {
                    return false
                }
            }
        }

        return true
    }
}