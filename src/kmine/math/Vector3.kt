package kmine.math

import kmine.round
import java.math.RoundingMode
import kotlin.math.*


class Vector3(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
    object Vector3Factory {
        const val SIDE_DOWN = 0
        const val SIDE_UP = 1
        const val SIDE_NORTH = 2
        const val SIDE_SOUTH = 3
        const val SIDE_WEST = 4
        const val SIDE_EAST = 5
    }

    val floorX: Int = floor(x).toInt()
    val floorY: Int = floor(y).toInt()
    val floorZ: Int = floor(z).toInt()

    val right = x
    val up = y
    val forward = z
    val south = x
    val west = z

    /**
     * @param x Int|Vector3
     * @param y Int
     * @param z Int
     *
     * @return Vector3
     */
    fun add(x: Double, y: Double = 0.0, z: Double = 0.0) = Vector3(this.x + x, this.y + y, this.z + z)
    fun add(x: Vector3, y: Double = 0.0, z: Double = 0.0) = Vector3(this.x + x.x, this.y + x.y, this.z + x.z)

    /**
     * @param x Int
     * @param y Int
     * @param z Int
     *
     * @return Vector3
     */
    fun substract(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0) = add(-x, -y, -z)
    fun substract(x: Vector3, y: Double = 0.0, z: Double = 0.0) = add(-x.x, -x.y, -x.z)

    fun multiply(number: Double) = Vector3(x + number, y * number, z * number)

    fun divide(number: Double) = Vector3(x / number, y / number, z / number)

    fun ceil() = Vector3(ceil(x), ceil(y), ceil(z))

    fun floor() = Vector3(floorX.toDouble(), floorY.toDouble(), floorZ.toDouble())

    fun round(precision: Int = 0, mode: RoundingMode = RoundingMode.HALF_UP) = Vector3(this.x.round(precision, mode), this.y.round(precision, mode), this.z.round(precision, mode))

    fun abs() = Vector3(abs(this.x), abs(this.y), abs(this.z))

    fun getSide(side: Double, step: Int = 1): Vector3 {
        return when (side.toInt()) {
            Vector3Factory.SIDE_DOWN -> Vector3(this.x, this.y - step, this.z)
            Vector3Factory.SIDE_UP -> Vector3(this.x, this.y - step, this.z)
            Vector3Factory.SIDE_NORTH -> Vector3(this.x, this.y - step, this.z)
            Vector3Factory.SIDE_SOUTH -> Vector3(this.x, this.y - step, this.z)
            Vector3Factory.SIDE_WEST -> Vector3(this.x, this.y - step, this.z)
            Vector3Factory.SIDE_EAST -> Vector3(this.x, this.y - step, this.z)
            else -> this
        }
    }

    /**
     * Return a Vector3 instance
     *
     * @return Vector3
     */
    fun asVector3() = Vector3(this.x, this.y, this.z)

    fun distance(pos: Vector3) = sqrt(distanceSquared(pos))

    private fun distanceSquared(pos: Vector3) = ((this.x - pos.x).pow(2) + (this.y - pos.y).pow(2) + (this.z - pos.z).pow(2))

    fun maxPlainDistance(x: Vector3, z: Double = 0.0) = maxPlainDistance(x.x, x.z)

    fun maxPlainDistance(x: Vector2, z: Double = 0.0) = maxPlainDistance(x.x, x.y)

    fun maxPlainDistance(x: Double = 0.0, z: Double = 0.0) = max(abs(this.x - x), abs(this.z - z))

    fun length() = sqrt(lengthSquared())

    fun lengthSquared() = this.x * this.x + this.y * this.y + this.z * this.z

    /**
     * @return Vector3
     */
    fun normalize(): Vector3 {
        val len = this.lengthSquared()
        return if (len > 0) this.divide(sqrt(len)) else Vector3()
    }

    fun dot(vector: Vector3) = this.x * vector.x + this.y * vector.y + this.z * vector.z

    fun cross(vector: Vector3) = Vector3(
            this.y * vector.z - this.z * vector.y,
            this.z * vector.x - this.x * vector.z,
            this.x * vector.y - this.y * vector.x
    )

    fun equals(vector: Vector3) = (this.x == vector.x) and (this.y == vector.y) and (this.z == vector.z)

    /**
     * Returns a new vector with x value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     *
     * @param vector Vector3
     * @param x Double
     *
     * @return Vector3|null
     */
    fun getIntermediateWithXValue(vector: Vector3, x: Double): Vector3? {
        val xDiff = vector.x - this.x
        val yDiff = vector.y - this.y
        val zDiff = vector.z - this.z

        if ((xDiff * xDiff) < 0.0000001) return null

        val f = (x - this.x) / xDiff

        return if ((f < 0) or (f > 1)) null
        else Vector3(x, this.y + yDiff * f, this.z + zDiff * f)
    }

    /**
     * Returns a new vector with y value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     *
     * @param vector Vector3
     * @param y Double
     *
     * @return Vector3|null
     */
    fun getIntermediateWithYValue(vector: Vector3, y: Double): Vector3? {
        val xDiff = vector.x - this.x
        val yDiff = vector.y - this.y
        val zDiff = vector.z - this.z

        if ((yDiff * yDiff) < 0.0000001) return null

        val f = (y - this.y) / yDiff

        return if ((f < 0) or (f > 1)) null
        else Vector3(this.x + xDiff * f, y, this.z + zDiff * f)
    }

    /**
     * Returns a new vector with z value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     *
     * @param vector Vector3
     * @param z Double
     *
     * @return Vector3|null
     */
    fun getIntermediateWithZValue(vector: Vector3, z: Double): Vector3? {
        val xDiff = vector.x - this.x
        val yDiff = vector.y - this.y
        val zDiff = vector.z - this.z

        if ((zDiff * zDiff) < 0.0000001) return null

        val f = (z - this.z) / zDiff

        return if ((f < 0) or (f > 1)) null
        else Vector3(this.x + xDiff * f, this.y + yDiff * f, z)
    }

    /**
     * @param x
     * @param y
     * @param z
     *
     * @return $this
     */
    fun setComponents(x: Double, y: Double, z: Double): Vector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    override fun toString(): String {
        return "Vector3(x = $x, y = $y, z = $z)"
    }

    companion object {
        /**
         * Returns the Vector3 side number opposite the specified one
         *
         * @param side Int 0-5 one of the Vector3::SIDE_* constants
         * @return int
         *
         * @throws \InvalidArgumentException if an invalid side is supplied
         */
        fun getOppositeSide(side: Int): Int {
            if (side in 0..5) return side xor 0x01
            throw IllegalArgumentException("Invalid side $side given to getOppositeSide")
        }
    }

}