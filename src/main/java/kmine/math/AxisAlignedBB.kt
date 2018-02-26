package kmine.math

import kmine.level.MovingObjectPosition

interface AxisAlignedBB: Cloneable {
    fun setBounds(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): AxisAlignedBB {
        this.setMinX(minX)
        this.setMinY(minY)
        this.setMinZ(minZ)
        this.setMaxX(maxX)
        this.setMaxY(maxY)
        this.setMaxZ(maxZ)
        return this
    }

    fun addCoord(x: Double, y: Double, z: Double): AxisAlignedBB {
        var minX = this.getMinX()
        var minY = this.getMinY()
        var minZ = this.getMinZ()
        var maxX = this.getMaxX()
        var maxY = this.getMaxY()
        var maxZ = this.getMaxZ()

        if (x < 0) minX += x
        if (x > 0) maxX += x

        if (y < 0) minY += y
        if (y > 0) maxY += y

        if (z < 0) minZ += z
        if (z > 0) maxZ += z

        return SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)
    }

    fun grow(x: Double, y: Double, z: Double): AxisAlignedBB {
        return SimpleAxisAlignedBB(this.getMinX() - x, this.getMinY() - y, this.getMinZ() - z, this.getMaxX() + x, this.getMaxY() + y, this.getMaxZ() + z)
    }

    fun expand(x: Double, y: Double, z: Double): AxisAlignedBB {
        this.setMinX(this.getMinX() - x)
        this.setMinY(this.getMinY() - y)
        this.setMinZ(this.getMinZ() - z)
        this.setMaxX(this.getMaxX() + x)
        this.setMaxY(this.getMaxY() + y)
        this.setMaxZ(this.getMaxZ() + z)

        return this
    }

    fun offset(x: Double, y: Double, z: Double): AxisAlignedBB {
        this.setMinX(this.getMinX() + x)
        this.setMinY(this.getMinY() + y)
        this.setMinZ(this.getMinZ() + z)
        this.setMaxX(this.getMaxX() + x)
        this.setMaxY(this.getMaxY() + y)
        this.setMaxZ(this.getMaxZ() + z)

        return this
    }

    fun shrink(x: Double, y: Double, z: Double): AxisAlignedBB {
        return SimpleAxisAlignedBB(this.getMinX() + x, this.getMinY() + y, this.getMinZ() + z, this.getMaxX() - x, this.getMaxY() - y, this.getMaxZ() - z)
    }

    fun contract(x: Double, y: Double, z: Double): AxisAlignedBB {
        this.setMinX(this.getMinX() + x)
        this.setMinY(this.getMinY() + y)
        this.setMinZ(this.getMinZ() + z)
        this.setMaxX(this.getMaxX() - x)
        this.setMaxY(this.getMaxY() - y)
        this.setMaxZ(this.getMaxZ() - z)

        return this
    }

    fun setBB(bb: AxisAlignedBB): AxisAlignedBB {
        this.setMinX(bb.getMinX())
        this.setMinY(bb.getMinY())
        this.setMinZ(bb.getMinZ())
        this.setMaxX(bb.getMaxX())
        this.setMaxY(bb.getMaxY())
        this.setMaxZ(bb.getMaxZ())
        return this
    }

    fun getOffsetBoundingBox(x: Double, y: Double, z: Double): AxisAlignedBB {
        return SimpleAxisAlignedBB(this.getMinX() + x, this.getMinY() + y, this.getMinZ() + z, this.getMaxX() + x, this.getMaxY() + y, this.getMaxZ() + z)
    }

    fun calculateXOffset(bb: AxisAlignedBB, x: Double): Double {
        var x = x
        if (bb.getMaxY() <= this.getMinY() || bb.getMinY() >= this.getMaxY()) {
            return x
        }
        if (bb.getMaxZ() <= this.getMinZ() || bb.getMinZ() >= this.getMaxZ()) {
            return x
        }
        if (x > 0 && bb.getMaxX() <= this.getMinX()) {
            val x1 = this.getMinX() - bb.getMaxX()
            if (x1 < x) {
                x = x1
            }
        }
        if (x < 0 && bb.getMinX() >= this.getMaxX()) {
            val x2 = this.getMaxX() - bb.getMinX()
            if (x2 > x) {
                x = x2
            }
        }

        return x
    }

    fun calculateYOffset(bb: AxisAlignedBB, y: Double): Double {
        var y = y
        if (bb.getMaxX() <= this.getMinX() || bb.getMinX() >= this.getMaxX()) {
            return y
        }
        if (bb.getMaxZ() <= this.getMinZ() || bb.getMinZ() >= this.getMaxZ()) {
            return y
        }
        if (y > 0 && bb.getMaxY() <= this.getMinY()) {
            val y1 = this.getMinY() - bb.getMaxY()
            if (y1 < y) {
                y = y1
            }
        }
        if (y < 0 && bb.getMinY() >= this.getMaxY()) {
            val y2 = this.getMaxY() - bb.getMinY()
            if (y2 > y) {
                y = y2
            }
        }

        return y
    }

    fun calculateZOffset(bb: AxisAlignedBB, z: Double): Double {
        var z = z
        if (bb.getMaxX() <= this.getMinX() || bb.getMinX() >= this.getMaxX()) {
            return z
        }
        if (bb.getMaxY() <= this.getMinY() || bb.getMinY() >= this.getMaxY()) {
            return z
        }
        if (z > 0 && bb.getMaxZ() <= this.getMinZ()) {
            val z1 = this.getMinZ() - bb.getMaxZ()
            if (z1 < z) {
                z = z1
            }
        }
        if (z < 0 && bb.getMinZ() >= this.getMaxZ()) {
            val z2 = this.getMaxZ() - bb.getMinZ()
            if (z2 > z) {
                z = z2
            }
        }

        return z
    }

    fun intersectsWith(bb: AxisAlignedBB): Boolean {
        if (bb.getMaxY() > this.getMinY() && bb.getMinY() < this.getMaxY()) {
            if (bb.getMaxX() > this.getMinX() && bb.getMinX() < this.getMaxX()) {
                return bb.getMaxZ() > this.getMinZ() && bb.getMinZ() < this.getMaxZ()
            }
        }

        return false
    }

    fun isVectorInside(vector: Vector3): Boolean {
        return vector.x >= this.getMinX() && vector.x <= this.getMaxX() && vector.y >= this.getMinY() && vector.y <= this.getMaxY() && vector.z >= this.getMinZ() && vector.z <= this.getMaxZ()

    }

    fun getAverageEdgeLength(): Double {
        return (this.getMaxX() - this.getMinX() + this.getMaxY() - this.getMinY() + this.getMaxZ() - this.getMinZ()) / 3
    }

    fun isVectorInYZ(vector: Vector3?): Boolean {
        return vector!!.y >= this.getMinY() && vector.y <= this.getMaxY() && vector.z >= this.getMinZ() && vector.z <= this.getMaxZ()
    }

    fun isVectorInXZ(vector: Vector3?): Boolean {
        return vector!!.x >= this.getMinX() && vector.x <= this.getMaxX() && vector.z >= this.getMinZ() && vector.z <= this.getMaxZ()
    }

    fun isVectorInXY(vector: Vector3?): Boolean {
        return vector!!.x >= this.getMinX() && vector.x <= this.getMaxX() && vector.y >= this.getMinY() && vector.y <= this.getMaxY()
    }

    fun calculateIntercept(pos1: Vector3, pos2: Vector3): MovingObjectPosition? {
        var v1 = pos1.getIntermediateWithXValue(pos2, this.getMinX())
        var v2 = pos1.getIntermediateWithXValue(pos2, this.getMaxX())
        var v3 = pos1.getIntermediateWithYValue(pos2, this.getMinY())
        var v4 = pos1.getIntermediateWithYValue(pos2, this.getMaxY())
        var v5 = pos1.getIntermediateWithZValue(pos2, this.getMinZ())
        var v6 = pos1.getIntermediateWithZValue(pos2, this.getMaxZ())

        if (v1 != null && !this.isVectorInYZ(v1)) {
            v1 = null
        }

        if (v2 != null && !this.isVectorInYZ(v2)) {
            v2 = null
        }

        if (v3 != null && !this.isVectorInXZ(v3)) {
            v3 = null
        }

        if (v4 != null && !this.isVectorInXZ(v4)) {
            v4 = null
        }

        if (v5 != null && !this.isVectorInXY(v5)) {
            v5 = null
        }

        if (v6 != null && !this.isVectorInXY(v6)) {
            v6 = null
        }

        var vector: Vector3? = null

        //if (v1 != null && (vector == null || pos1.distanceSquared(v1) < pos1.distanceSquared(vector))) {
        if (v1 != null) {
            vector = v1
        }

        if (v2 != null && (vector == null || pos1.distanceSquared(v2) < pos1.distanceSquared(vector))) {
            vector = v2
        }

        if (v3 != null && (vector == null || pos1.distanceSquared(v3) < pos1.distanceSquared(vector))) {
            vector = v3
        }

        if (v4 != null && (vector == null || pos1.distanceSquared(v4) < pos1.distanceSquared(vector))) {
            vector = v4
        }

        if (v5 != null && (vector == null || pos1.distanceSquared(v5) < pos1.distanceSquared(vector))) {
            vector = v5
        }

        if (v6 != null && (vector == null || pos1.distanceSquared(v6) < pos1.distanceSquared(vector))) {
            vector = v6
        }

        if (vector == null) {
            return null
        }

        var face = -1

        when (vector) {
            v1 -> face = 4
            v2 -> face = 5
            v3 -> face = 0
            v4 -> face = 1
            v5 -> face = 2
            v6 -> face = 3
        }

        return MovingObjectPosition.fromBlock(0, 0, 0, face, vector)
    }

    fun setMinX(minX: Double) {
        throw UnsupportedOperationException("Not mutable")
    }

    fun setMinY(minY: Double) {
        throw UnsupportedOperationException("Not mutable")
    }

    fun setMinZ(minZ: Double) {
        throw UnsupportedOperationException("Not mutable")
    }

    fun setMaxX(maxX: Double) {
        throw UnsupportedOperationException("Not mutable")
    }

    fun setMaxY(maxY: Double) {
        throw UnsupportedOperationException("Not mutable")
    }

    fun setMaxZ(maxZ: Double) {
        throw UnsupportedOperationException("Not mutable")
    }


    fun getMinX(): Double
    fun getMinY(): Double
    fun getMinZ(): Double
    fun getMaxX(): Double
    fun getMaxY(): Double
    fun getMaxZ(): Double

    public override fun clone(): AxisAlignedBB
}