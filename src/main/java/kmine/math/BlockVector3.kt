package kmine.math

class BlockVector3: Cloneable {
    var x: Int = 0
    var y: Int = 0
    var z: Int = 0

    constructor(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor() {}

    fun setComponents(x: Int, y: Int, z: Int): BlockVector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun add(x: Double): Vector3 {
        return this.add(x, 0.0, 0.0)
    }

    fun add(x: Double, y: Double): Vector3 {
        return this.add(x, y, 0.0)
    }

    fun add(x: Double, y: Double, z: Double): Vector3 {
        return Vector3(this.x + x, this.y + y, this.z + z)
    }

    fun add(x: Vector3): Vector3 {
        return Vector3(this.x + x.x, this.y + x.y, this.z + x.z)
    }

    fun subtract(x: Double): Vector3 {
        return this.subtract(x, 0.0, 0.0)
    }

    fun subtract(x: Double, y: Double): Vector3 {
        return this.subtract(x, y, 0.0)
    }

    fun subtract(x: Double, y: Double, z: Double): Vector3 {
        return this.add(-x, -y, -z)
    }

    fun subtract(x: Vector3): Vector3 {
        return this.add(-x.x, -x.y, -x.z)
    }

    fun add(x: Int): BlockVector3 {
        return this.add(x, 0, 0)
    }

    fun add(x: Int, y: Int): BlockVector3 {
        return this.add(x, y, 0)
    }

    fun add(x: Int, y: Int, z: Int): BlockVector3 {
        return BlockVector3(this.x + x, this.y + y, this.z + z)
    }

    fun add(x: BlockVector3): BlockVector3 {
        return BlockVector3(this.x + x.x, this.y + x.y, this.z + x.z)
    }

    fun subtract(): BlockVector3 {
        return this.subtract(0, 0, 0)
    }

    fun subtract(x: Int): BlockVector3 {
        return this.subtract(x, 0, 0)
    }

    fun subtract(x: Int, y: Int): BlockVector3 {
        return this.subtract(x, y, 0)
    }

    fun subtract(x: Int, y: Int, z: Int): BlockVector3 {
        return this.add(-x, -y, -z)
    }

    fun subtract(x: BlockVector3): BlockVector3 {
        return this.add(-x.x, -x.y, -x.z)
    }

    fun multiply(number: Int): BlockVector3 {
        return BlockVector3(this.x * number, this.y * number, this.z * number)
    }

    fun divide(number: Int): BlockVector3 {
        return BlockVector3(this.x / number, this.y / number, this.z / number)
    }

    fun getSide(face: BlockFace): BlockVector3 {
        return this.getSide(face, 1)
    }

    fun getSide(face: BlockFace, step: Int): BlockVector3 {
        return BlockVector3(this.x + face.getXOffset() * step, this.y + face.getYOffset() * step, this.z + face.getZOffset() * step)
    }

    fun up(): BlockVector3 {
        return up(1)
    }

    fun up(step: Int): BlockVector3 {
        return getSide(BlockFace.UP, step)
    }

    fun down(): BlockVector3 {
        return down(1)
    }

    fun down(step: Int): BlockVector3 {
        return getSide(BlockFace.DOWN, step)
    }

    fun north(): BlockVector3 {
        return north(1)
    }

    fun north(step: Int): BlockVector3 {
        return getSide(BlockFace.NORTH, step)
    }

    fun south(): BlockVector3 {
        return south(1)
    }

    fun south(step: Int): BlockVector3 {
        return getSide(BlockFace.SOUTH, step)
    }

    fun east(): BlockVector3 {
        return east(1)
    }

    fun east(step: Int): BlockVector3 {
        return getSide(BlockFace.EAST, step)
    }

    fun west(): BlockVector3 {
        return west(1)
    }

    fun west(step: Int): BlockVector3 {
        return getSide(BlockFace.WEST, step)
    }

    fun distance(pos: Vector3): Double {
        return Math.sqrt(this.distanceSquared(pos))
    }

    fun distance(pos: BlockVector3): Double {
        return Math.sqrt(this.distanceSquared(pos))
    }

    fun distanceSquared(pos: Vector3): Double {
        return distanceSquared(pos.x, pos.y, pos.z)
    }

    fun distanceSquared(pos: BlockVector3): Double {
        return distanceSquared(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
    }

    fun distanceSquared(x: Double, y: Double, z: Double): Double {
        return Math.pow(this.x - x, 2.0) + Math.pow(this.y - y, 2.0) + Math.pow(this.z - z, 2.0)
    }

    override fun equals(ob: Any?): Boolean {
        if (ob == null) return false
        if (ob === this) return true

        return if (ob !is BlockVector3) false else this.x == ob.x &&
                this.y == ob.y &&
                this.z == ob.z

    }

    override fun hashCode(): Int {
        return x xor (z shl 12) xor (y shl 24)
    }

    override fun toString(): String {
        return "BlockPosition(level=" + ",x=" + this.x + ",y=" + this.y + ",z=" + this.z + ")"
    }

    public override fun clone(): Any {
        return try {
            super.clone() as BlockVector3
        } catch (e: CloneNotSupportedException) {
            ""
        }

    }

    fun asVector3(): Vector3 {
        return Vector3(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
    }

    fun asVector3f(): Vector3f {
        return Vector3f(this.x.toFloat(), this.y.toFloat(), this.z.toFloat())
    }
}