package kmine.math

import java.util.*

/**
 * Double
 */
fun Double.floor(): Int {
    val i = this.toInt()
    return if (this < i.toDouble()) i - 1 else i
}

fun Double.floorDoubleLong(): Long {
    val l = this.toLong()
    return if (this >= l.toDouble()) l else l - 1L
}

/**
 *  Int
 */
fun Int.abs(): Int {
    return if (this > 0) {
        this
    } else {
        -this
    }
}

fun Int.log2nlz(): Int {
    return if (this == 0) 0 else 31 - Integer.numberOfLeadingZeros(this)
}

fun Int.clamp(min: Int, max: Int): Int {
    return if (this > max) max else if (this < min) min else this
}

/**
 * Float
 */
fun Float.ceil(): Int {
    val truncated = this.toInt()
    return if (this > truncated) truncated + 1 else truncated
}

fun Float.sqrt(): Float {
    return Math.sqrt(this.toDouble()).toFloat()
}

fun Float.floor(): Int {
    val i = this.toInt()
    return if (this < i.toDouble()) i - 1 else i
}

/**
 * Returns a random number between min and max, inclusive.
 *
 * @this The random number generator.
 * @param min    The minimum value.
 * @param max    The maximum value.
 * @return A random number between min and max, inclusive.
 */
fun Random.getRandomNumberInRange(min: Int, max: Int): Int {
    return min + this.nextInt(max - min + 1)
}

class MathHelper {
    companion object {
        private val a = FloatArray(65536)

        init {
            for (i in 0 until 65536)
                a[i] = Math.sin(i * 3.141592653589793 * 2.0 / 65536.0).toFloat()
        }

        fun sin(paramFloat: Float): Float {
            return a[(paramFloat * 10430.378f).toInt() and 0xFFFF]
        }

        fun cos(paramFloat: Float): Float {
            return a[(paramFloat * 10430.378f + 16384.0f).toInt() and 0xFFFF]
        }

        fun max(vararg number: Double): Double = number.max()!!
    }
}