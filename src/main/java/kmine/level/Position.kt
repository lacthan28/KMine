package kmine.level

import com.sun.javaws.exceptions.InvalidArgumentException
import kmine.math.Vector3

internal class Position @JvmOverloads constructor(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0, var level: Level? = null) : Vector3() {

    val isValid: Boolean
        get() = this.level != null

    val levelBlock: Block
        get() = if (this.isValid)
            this.level!!.getBlock(this)
        else
            throw LevelException("Undefined Level reference")

    val location: Location
        get() = if (this.isValid)
            Location(this.x, this.y, this.z, 0, 0, this.level)
        else
            throw LevelException("Undefined Level reference")

    init {
        this.x = x
        this.y = y
        this.z = z
    }

    fun setLevel(level: Level): Position {
        this.level = level
        return this
    }

    fun setStrong(): Boolean {
        return false
    }

    fun setWeak(): Boolean {
        return false
    }

    fun getSide(face: BlockFace): Position {
        return this.getSide(face, 1)
    }

    fun getSide(face: BlockFace, step: Int): Position {
        if (!this.isValid) {
            throw LevelException("Undefined Level reference")
        }
        return Position.fromObject(super.getSide(face, step), this.level)
    }

    fun toString(): String {
        return "Position(level=" + (if (this.isValid) this.getLevel()!!.getName() else "null") + ",x=" + this.x + ",y=" + this.y + ",z=" + this.z + ")"
    }

    fun setComponents(x: Double, y: Double, z: Double): Position {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun add(x: Double): Position {
        return this.add(x, 0.0, 0.0)
    }

    fun add(x: Double, y: Double): Position {
        return this.add(x, y, 0.0)
    }

    fun add(x: Double, y: Double, z: Double): Position {
        return Position(this.x + x, this.y + y, this.z + z, this.level)
    }

    fun add(x: Vector3): Position {
        return Position(this.x + x.getX(), this.y + x.getY(), this.z + x.getZ(), this.level)
    }

    fun subtract(): Position {
        return this.subtract(0.0, 0.0, 0.0)
    }

    fun subtract(x: Double): Position {
        return this.subtract(x, 0.0, 0.0)
    }

    fun subtract(x: Double, y: Double): Position {
        return this.subtract(x, y, 0.0)
    }

    fun subtract(x: Double, y: Double, z: Double): Position {
        return this.add(-x, -y, -z)
    }

    fun subtract(x: Vector3): Position {
        return this.add(-x.getX(), -x.getY(), -x.getZ())
    }

    fun multiply(number: Double): Position {
        return Position(this.x * number, this.y * number, this.z * number, this.level)
    }

    fun divide(number: Double): Position {
        return Position(this.x / number, this.y / number, this.z / number, this.level)
    }

    fun ceil(): Position {
        return Position(Math.ceil(this.x).toInt().toDouble(), Math.ceil(this.y).toInt().toDouble(), Math.ceil(this.z).toInt().toDouble(), this.level)
    }

    fun floor(): Position {
        return Position(this.getFloorX(), this.getFloorY(), this.getFloorZ(), this.level)
    }

    fun round(): Position {
        return Position(Math.round(this.x), Math.round(this.y), Math.round(this.z), this.level)
    }

    fun abs(): Position {
        return Position((Math.abs(this.x) as Int).toDouble(), (Math.abs(this.y) as Int).toDouble(), (Math.abs(this.z) as Int).toDouble(), this.level)
    }

    fun clone(): Position {
        return super.clone() as Position
    }

    companion object {

        @JvmOverloads
        fun fromObject(pos: Vector3, level: Level? = null): Position {
            return Position(pos.x, pos.y, pos.z, level)
        }
    }
}