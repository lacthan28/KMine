package kmine.level

import com.sun.javaws.exceptions.InvalidArgumentException
import kmine.math.Vector3

open class Position(final override var x: Double = 0.0, final override var y: Double = 0.0, final override var z: Double = 0.0, level: Level? = null) : Vector3() {
    companion object {
        fun fromObject(pos: Vector3, level: Level? = null) = Position(pos.x, pos.y, pos.z, level)
    }

    fun asPosition() = Position(this.x, this.y, this.z, this.level)

    var level:Level? = null
    get() {
        if (field != null && field!!.closed) {
            //Log debug "Position was holding a reference to an unloaded Level"
            level = null
        }
        return field
    }

    fun setLevel(level: Level? = null): Position {
        if (level != null && level.closed) {
            throw InvalidArgumentException(arrayOf("Specified level has been unloaded and cannot be used"))
        }

        this.level = level
        return this
    }

    fun isValid() = level is Level

    override fun getSide(side: Int, step: Int): Position {
        assert(isValid())
        return Position.fromObject(super.getSide(side, step), this.level)
    }

    override fun toString(): String = "Position(level=${if (this.isValid()) this.level?.displayName else "null"},x=$x,y=$y,z=$z)"

    override fun equals(other: Any?): Boolean {
        if (other is Position) return super.equals(other) && other.level == this.level
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + (level?.hashCode() ?: 0)
        return result
    }
}