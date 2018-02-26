package kmine.math

class Vector3f(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) : Cloneable {
    companion object {
        val SIDE_DOWN = 0
        val SIDE_UP = 1
        val SIDE_NORTH = 2
        val SIDE_SOUTH = 3
        val SIDE_WEST = 4
        val SIDE_EAST = 5
    }

    fun getFloorX(): Int {
        return this.x.floor()
    }

    fun getFloorY(): Int {
        return this.y.floor()
    }

    fun getFloorZ(): Int {
        return this.z.floor()
    }

    fun getRight(): Float {
        return this.x
    }

    fun getUp(): Float {
        return this.y
    }

    fun getForward(): Float {
        return this.z
    }

    fun getSouth(): Float {
        return this.x
    }

    fun getWest(): Float {
        return this.z
    }

    fun add(x: Float, y: Float = 0f, z: Float = 0f): Vector3f {
        return Vector3f(this.x + x, this.y + y, this.z + z)
    }

    fun Vector3f.add(): Vector3f {
        return Vector3f(x + this.x, y + this.y, z + this.z)
    }

    fun subtract(x: Float = 0f, y: Float = 0f, z: Float = 0f): Vector3f {
        return this.add(-x, -y, -z)
    }

    fun Vector3f.subtract(): Vector3f {
        return add(-this.x, -this.y, -this.z)
    }

    fun multiply(number: Float): Vector3f {
        return Vector3f(this.x * number, this.y * number, this.z * number)
    }

    fun divide(number: Float): Vector3f {
        return Vector3f(this.x / number, this.y / number, this.z / number)
    }

    fun ceil(): Vector3f {
        return Vector3f(Math.ceil(this.x.toDouble()).toInt().toFloat(), Math.ceil(this.y.toDouble()).toInt().toFloat(), Math.ceil(this.z.toDouble()).toInt().toFloat())
    }

    fun floor(): Vector3f {
        return Vector3f(this.getFloorX().toFloat(), this.getFloorY().toFloat(), this.getFloorZ().toFloat())
    }

    fun round(): Vector3f {
        return Vector3f(Math.round(this.x).toFloat(), Math.round(this.y).toFloat(), Math.round(this.z).toFloat())
    }

    fun abs(): Vector3f {
        return Vector3f(Math.abs(this.x).toInt().toFloat(), Math.abs(this.y).toInt().toFloat(), Math.abs(this.z).toInt().toFloat())
    }

    fun getSide(side: Int): Vector3f {
        return this.getSide(side, 1)
    }

    fun getSide(side: Int, step: Int): Vector3f {
        when (side) {
            Vector3f.SIDE_DOWN -> return Vector3f(this.x, this.y - step, this.z)
            Vector3f.SIDE_UP -> return Vector3f(this.x, this.y + step, this.z)
            Vector3f.SIDE_NORTH -> return Vector3f(this.x, this.y, this.z - step)
            Vector3f.SIDE_SOUTH -> return Vector3f(this.x, this.y, this.z + step)
            Vector3f.SIDE_WEST -> return Vector3f(this.x - step, this.y, this.z)
            Vector3f.SIDE_EAST -> return Vector3f(this.x + step, this.y, this.z)
            else -> return this
        }
    }

    fun getOppositeSide(side: Int): Int {
        when (side) {
            Vector3f.SIDE_DOWN -> return Vector3f.SIDE_UP
            Vector3f.SIDE_UP -> return Vector3f.SIDE_DOWN
            Vector3f.SIDE_NORTH -> return Vector3f.SIDE_SOUTH
            Vector3f.SIDE_SOUTH -> return Vector3f.SIDE_NORTH
            Vector3f.SIDE_WEST -> return Vector3f.SIDE_EAST
            Vector3f.SIDE_EAST -> return Vector3f.SIDE_WEST
            else -> return -1
        }
    }

    fun Vector3f.distance(): Double {
        return Math.sqrt(this.distanceSquared())
    }

    fun Vector3f.distanceSquared(): Double {
        return Math.pow((x - this.x).toDouble(), 2.0) + Math.pow((y - this.y).toDouble(), 2.0) + Math.pow((z - this.z).toDouble(), 2.0)
    }

    fun maxPlainDistance(x: Float = 0f, z: Float = 0f): Float {
        return Math.max(Math.abs(this.x - x), Math.abs(this.z - z))
    }

    fun Vector2f.maxPlainDistance(): Float {
        return maxPlainDistance(this.x, this.y)
    }

    fun Vector3f.maxPlainDistance(): Float {
        return this.maxPlainDistance(this.x, this.z)
    }

    fun length(): Double {
        return Math.sqrt(this.lengthSquared().toDouble())
    }

    fun lengthSquared(): Float {
        return this.x * this.x + this.y * this.y + this.z * this.z
    }

    fun normalize(): Vector3f {
        val len = this.lengthSquared()
        return if (len > 0) {
            this.divide(Math.sqrt(len.toDouble()).toFloat())
        } else Vector3f(0f, 0f, 0f)
    }

    fun Vector3f.dot(): Float {
        return this.x * x + this.y * y + this.z * z
    }

    fun Vector3f.cross(): Vector3f {
        return Vector3f(
                y * this.z - z * this.y,
                z * this.x - x * this.z,
                x * this.y - y * this.x
        )
    }

    /**
     * Returns a new vector with x value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    fun Vector3f.getIntermediateWithXValue(x: Float): Vector3f? {
        val xDiff = this.x - x
        val yDiff = this.y - y
        val zDiff = this.z - z
        if (xDiff * xDiff < 0.0000001) {
            return null
        }
        val f = (x - x) / xDiff
        return if (f < 0 || f > 1) {
            null
        } else {
            Vector3f(x + xDiff * f, y + yDiff * f, z + zDiff * f)
        }
    }

    /**
     * Returns a new vector with y value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    fun Vector3f.getIntermediateWithYValue(y: Float): Vector3f? {
        val xDiff = this.x - x
        val yDiff = this.y - y
        val zDiff = this.z - z
        if (yDiff * yDiff < 0.0000001) {
            return null
        }
        val f = (y - y) / yDiff
        return if (f < 0 || f > 1) {
            null
        } else {
            Vector3f(x + xDiff * f, y + yDiff * f, z + zDiff * f)
        }
    }

    /**
     * Returns a new vector with z value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    fun Vector3f.getIntermediateWithZValue(z: Float): Vector3f? {
        val xDiff = this.x - x
        val yDiff = this.y - y
        val zDiff = this.z - z
        if (zDiff * zDiff < 0.0000001) {
            return null
        }
        val f = (z - z) / zDiff
        return if (f < 0 || f > 1) {
            null
        } else {
            Vector3f(x + xDiff * f, y + yDiff * f, z + zDiff * f)
        }
    }

    fun setComponents(x: Float, y: Float, z: Float): Vector3f {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    override fun toString(): String {
        return "Vector3(x=${this.x},y=${this.y},z=${this.z})"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Vector3f) {
            return false
        }

        val other = other as Vector3f?

        return this.x == other!!.x && this.y == other.y && this.z == other.z
    }

    fun rawHashCode(): Int {
        return super.hashCode()
    }

    public override fun clone(): Any {
        return try {
            super.clone() as Vector3f
        } catch (e: CloneNotSupportedException) {
            ""
        }

    }

    fun asVector3(): Vector3 {
        return Vector3(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
    }

    fun asBlockVector3(): BlockVector3 {
        return BlockVector3(getFloorX(), getFloorY(), getFloorZ())
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }
}