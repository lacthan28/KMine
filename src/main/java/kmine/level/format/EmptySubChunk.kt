package kmine.level.format

class EmptySubChunk : SubChunkInterface {
    override fun isEmpty(checkLight: Boolean): Boolean = true

    override fun getBlockId(x: Int, y: Int, z: Int): Int = 0

    override fun setBlockId(x: Int, y: Int, z: Int, id: Int): Boolean = false

    override fun getBlockData(x: Int, y: Int, z: Int): Int = 0

    override fun setBlockData(x: Int, y: Int, z: Int, data: Int): Boolean = false

    override fun getFullBlock(x: Int, y: Int, z: Int): Int = 0

    override fun setBlock(x: Int, y: Int, z: Int, id: Int?, data: Int?): Boolean = false

    override fun getBlockLight(x: Int, y: Int, z: Int): Int = 0

    override fun setBlockLight(x: Int, y: Int, z: Int, level: Int): Boolean = false

    override fun getBlockSkyLight(x: Int, y: Int, z: Int): Int = 15

    override fun setBlockSkyLight(x: Int, y: Int, z: Int, level: Int): Boolean = false

    override fun getHighestBlockAt(x: Int, z: Int): Int = -1

    override fun getBlockIdColumn(x: Int, z: Int): String = "\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00"

    override fun getBlockDataColumn(x: Int, z: Int): String = "\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00"

    override fun getBlockLightColumn(x: Int, z: Int): String = "\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00"

    override fun getBlockSkyLightColumn(x: Int, z: Int): String = "\\xff\\xff\\xff\\xff\\xff\\xff\\xff\\xff"

    override fun getBlockIdArray(): String = "\\x00".repeat(4096)

    override fun getBlockDataArray(): String = "\\x00".repeat(2048)

    override fun getBlockSkyLightArray(): String = "\\xff".repeat(2048)

    override fun setBlockSkyLightArray(data: String) {

    }

    override fun getBlockLightArray(): String = "\\x00".repeat(2048)

    override fun setBlockLightArray(data: String) {

    }

    override fun networkSerialize(): String = "\\x00${"\\x00".repeat(6144)}"

    override fun fastSerialize(): String {
        throw IllegalCallerException("Should not try to serialize empty subchunks")
    }
}