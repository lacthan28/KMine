package kmine.math

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

class MRandom {
    private var seed: Long = 0L

    constructor() {
        MRandom(-1)
    }

    constructor(seeds: Long) {
        var seed = seeds
        if (seed == -1L) {
            seed = System.currentTimeMillis() / 1000L
        }
        this.setSeed(seeds)
    }

    fun setSeed(seeds: Long) {
        val crc32 = CRC32()
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(seeds.toInt())
        crc32.update(buffer.array())
        this.seed = crc32.value
    }

    fun nextSignedInt(): Int {
        val t = ((this.seed * 65535 + 31337).toInt() shr 8) + 1337
        this.seed = this.seed xor t.toLong()
        return t
    }

    fun nextInt(): Int {
        return this.nextSignedInt() and 0x7fffffff
    }

    fun nextDouble(): Double {
        return this.nextInt().toDouble() / 0x7fffffff
    }

    fun nextFloat(): Float {
        return this.nextInt().toFloat() / 0x7fffffff
    }

    fun nextSignedFloat(): Float {
        return this.nextInt().toFloat() / 0x7fffffff
    }

    fun nextSignedDouble(): Double {
        return this.nextSignedInt().toDouble() / 0x7fffffff
    }

    fun nextBoolean(): Boolean {
        return this.nextSignedInt() and 0x01 == 0
    }

    fun nextRange(): Int {
        return nextRange(0, 0x7fffffff)
    }

    fun nextRange(start: Int): Int {
        return nextRange(start, 0x7fffffff)
    }

    fun nextRange(start: Int, end: Int): Int {
        return start + this.nextInt() % (end + 1 - start)
    }

    fun nextBoundedInt(bound: Int): Int {
        return this.nextInt() % bound
    }
}