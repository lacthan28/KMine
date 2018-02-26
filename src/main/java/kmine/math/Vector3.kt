package kmine.math

class Vector3: Cloneable {

    var x: Double = 0.toDouble()
    var y: Double = 0.toDouble()
    var z: Double = 0.toDouble()

    constructor(){
        Vector3(0.0, 0.0, 0.0)
    }

    constructor(x: Double) {
        Vector3(x, 0.0, 0.0)
    }

    constructor(x: Double, y: Double) {
        Vector3(x, y, 0.0)
    }

    constructor(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun getFloorX(): Int {
        return Math.floor(this.x).toInt()
    }

    fun getFloorY(): Int {
        return Math.floor(this.y).toInt()
    }

    fun getFloorZ(): Int {
        return Math.floor(this.z).toInt()
    }

    fun getRight(): Double {
        return this.x
    }

    fun getUp(): Double {
        return this.y
    }

    fun getForward(): Double {
        return this.z
    }

    fun getSouth(): Double {
        return this.x
    }

    fun getWest(): Double {
        return this.z
    }

    fun add(x: Double, y: Double = 0.0, z: Double = 0.0): Vector3 {
        return Vector3(this.x + x, this.y + y, this.z + z)
    }

    fun add(x: Vector3): Vector3 {
        return Vector3(this.x + x.x, this.y + x.y, this.z + x.z)
    }

    fun subtract(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vector3 {
        return this.add(-x, -y, -z)
    }

    fun subtract(x: Vector3): Vector3 {
        return this.add(-x.x, -x.y, -x.z)
    }

    fun multiply(number: Double): Vector3 {
        return Vector3(this.x * number, this.y * number, this.z * number)
    }

    fun divide(number: Double): Vector3 {
        return Vector3(this.x / number, this.y / number, this.z / number)
    }

    fun ceil(): Vector3 {
        return Vector3(Math.ceil(this.x).toInt().toDouble(), Math.ceil(this.y).toInt().toDouble(), Math.ceil(this.z).toInt().toDouble())
    }

    fun floor(): Vector3 {
        return Vector3(this.getFloorX().toDouble(), this.getFloorY().toDouble(), this.getFloorZ().toDouble())
    }

    fun round(): Vector3 {
        return Vector3(Math.round(this.x).toDouble(), Math.round(this.y).toDouble(), Math.round(this.z).toDouble())
    }

    fun abs(): Vector3 {
        return Vector3(Math.abs(this.x).toInt().toDouble(), Math.abs(this.y).toInt().toDouble(), Math.abs(this.z).toInt().toDouble())
    }

    fun getSide(face: BlockFace): Vector3 {
        return this.getSide(face, 1)
    }

    fun getSide(face: BlockFace, step: Int): Vector3 {
        return Vector3(this.x + face.getXOffset() * step, this.y + face.getYOffset() * step, this.z + face.getZOffset() * step)
    }

    fun up(): Vector3 {
        return up(1)
    }

    fun up(step: Int): Vector3 {
        return getSide(BlockFace.UP, step)
    }

    fun down(): Vector3 {
        return down(1)
    }

    fun down(step: Int): Vector3 {
        return getSide(BlockFace.DOWN, step)
    }

    fun north(): Vector3 {
        return north(1)
    }

    fun north(step: Int): Vector3 {
        return getSide(BlockFace.NORTH, step)
    }

    fun south(): Vector3 {
        return south(1)
    }

    fun south(step: Int): Vector3 {
        return getSide(BlockFace.SOUTH, step)
    }

    fun east(): Vector3 {
        return east(1)
    }

    fun east(step: Int): Vector3 {
        return getSide(BlockFace.EAST, step)
    }

    fun west(): Vector3 {
        return west(1)
    }

    fun west(step: Int): Vector3 {
        return getSide(BlockFace.WEST, step)
    }

    fun distance(pos: Vector3): Double {
        return Math.sqrt(this.distanceSquared(pos))
    }

    fun distanceSquared(pos: Vector3): Double {
        return Math.pow(this.x - pos.x, 2.0) + Math.pow(this.y - pos.y, 2.0) + Math.pow(this.z - pos.z, 2.0)
    }

    fun maxPlainDistance(): Double {
        return this.maxPlainDistance(0.0, 0.0)
    }

    fun maxPlainDistance(x: Double): Double {
        return this.maxPlainDistance(x, 0.0)
    }

    fun maxPlainDistance(x: Double, z: Double): Double {
        return Math.max(Math.abs(this.x - x), Math.abs(this.z - z))
    }

    fun maxPlainDistance(vector: Vector2): Double {
        return this.maxPlainDistance(vector.x, vector.y)
    }

    fun maxPlainDistance(x: Vector3): Double {
        return this.maxPlainDistance(x.x, x.z)
    }

    fun length(): Double {
        return Math.sqrt(this.lengthSquared())
    }

    fun lengthSquared(): Double {
        return this.x * this.x + this.y * this.y + this.z * this.z
    }

    fun normalize(): Vector3 {
        val len = this.lengthSquared()
        return if (len > 0) {
            this.divide(Math.sqrt(len))
        } else Vector3(0.0, 0.0, 0.0)
    }

    fun dot(v: Vector3): Double {
        return this.x * v.x + this.y * v.y + this.z * v.z
    }

    fun cross(v: Vector3): Vector3 {
        return Vector3(
                this.y * v.z - this.z * v.y,
                this.z * v.x - this.x * v.z,
                this.x * v.y - this.y * v.x
        )
    }

    /**
     * Returns a new vector with x value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    fun getIntermediateWithXValue(v: Vector3, x: Double): Vector3? {
        val xDiff = v.x - this.x
        val yDiff = v.y - this.y
        val zDiff = v.z - this.z
        if (xDiff * xDiff < 0.0000001) {
            return null
        }
        val f = (x - this.x) / xDiff
        return if (f < 0 || f > 1) {
            null
        } else {
            Vector3(this.x + xDiff * f, this.y + yDiff * f, this.z + zDiff * f)
        }
    }

    /**
     * Returns a new vector with y value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    fun getIntermediateWithYValue(v: Vector3, y: Double): Vector3? {
        val xDiff = v.x - this.x
        val yDiff = v.y - this.y
        val zDiff = v.z - this.z
        if (yDiff * yDiff < 0.0000001) {
            return null
        }
        val f = (y - this.y) / yDiff
        return if (f < 0 || f > 1) {
            null
        } else {
            Vector3(this.x + xDiff * f, this.y + yDiff * f, this.z + zDiff * f)
        }
    }

    /**
     * Returns a new vector with z value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    fun getIntermediateWithZValue(v: Vector3, z: Double): Vector3? {
        val xDiff = v.x - this.x
        val yDiff = v.y - this.y
        val zDiff = v.z - this.z
        if (zDiff * zDiff < 0.0000001) {
            return null
        }
        val f = (z - this.z) / zDiff
        return if (f < 0 || f > 1) {
            null
        } else {
            Vector3(this.x + xDiff * f, this.y + yDiff * f, this.z + zDiff * f)
        }
    }

    fun setComponents(x: Double, y: Double, z: Double): Vector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    override fun toString(): String {
        return "Vector3(x=" + this.x + ",y=" + this.y + ",z=" + this.z + ")"
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is Vector3) {
            return false
        }

        val other = obj as Vector3?

        return this.x == other!!.x && this.y == other.y && this.z == other.z
    }

    override fun hashCode(): Int {
        return x.toInt() xor (z.toInt() shl 12) xor (y.toInt() shl 24)
    }

    fun rawHashCode(): Int {
        return super.hashCode()
    }

    override fun clone(): Any {
        return try {
            super.clone()
        } catch (e: CloneNotSupportedException) {
            ""
        }

    }

    fun asVector3f(): Vector3f {
        return Vector3f(this.x.toFloat(), this.y.toFloat(), this.z.toFloat())
    }

    fun asBlockVector3(): BlockVector3 {
        return BlockVector3(this.getFloorX(), this.getFloorY(), this.getFloorZ())
    }
}