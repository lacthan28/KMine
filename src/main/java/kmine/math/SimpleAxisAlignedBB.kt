package kmine.math

class SimpleAxisAlignedBB : AxisAlignedBB {
    private var minX: Double = 0.toDouble()
    private var minY: Double = 0.toDouble()
    private var minZ: Double = 0.toDouble()
    private var maxX: Double = 0.toDouble()
    private var maxY: Double = 0.toDouble()
    private var maxZ: Double = 0.toDouble()

    constructor(pos1: Vector3, pos2: Vector3) {
        this.minX = Math.min(pos1.x, pos2.x)
        this.minY = Math.min(pos1.y, pos2.y)
        this.minZ = Math.min(pos1.z, pos2.z)
        this.maxX = Math.max(pos1.x, pos2.x)
        this.maxY = Math.max(pos1.y, pos2.y)
        this.maxZ = Math.max(pos1.z, pos2.z)
    }

    constructor(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) {
        this.minX = minX
        this.minY = minY
        this.minZ = minZ
        this.maxX = maxX
        this.maxY = maxY
        this.maxZ = maxZ
    }

    override fun toString(): String {
        return "AxisAlignedBB(${this.getMinX()}, ${this.getMinY()}, ${this.getMinZ()}, ${this.getMaxX()}, ${this.getMaxY()}, ${this.getMaxZ()})"
    }

    override fun getMinX(): Double {
        return minX
    }

    override fun setMinX(minX: Double) {
        this.minX = minX
    }

    override fun getMinY(): Double {
        return minY
    }

    override fun setMinY(minY: Double) {
        this.minY = minY
    }

    override fun getMinZ(): Double {
        return minZ
    }

    override fun setMinZ(minZ: Double) {
        this.minZ = minZ
    }

    override fun getMaxX(): Double {
        return maxX
    }

    override fun setMaxX(maxX: Double) {
        this.maxX = maxX
    }

    override fun getMaxY(): Double {
        return maxY
    }

    override fun setMaxY(maxY: Double) {
        this.maxY = maxY
    }

    override fun getMaxZ(): Double {
        return maxZ
    }

    override fun setMaxZ(maxZ: Double) {
        this.maxZ = maxZ
    }

    override fun clone(): AxisAlignedBB {
        return SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)
    }
}