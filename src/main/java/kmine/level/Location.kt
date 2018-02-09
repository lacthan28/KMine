package kmine.level

import kmine.math.Vector3

/**
 * @param int   x
 * @param int   y
 * @param int   z
 * @param float yaw
 * @param float pitch
 * @param Level level
 */
class Location(x: Int = 0, y: Int = 0, z: Int = 0,
               public var yaw: Float = 0f,
               public var pitch: Float = 0f, level: Level? = null) : Position() {

    companion object {
        /**
         * @param Vector3    pos
         * @param Level|null level default null
         * @param float      yaw   default 0.0
         * @param float      pitch default 0.0
         *
         * @return Location
         */
        fun fromObject(pos: Vector3, level: Level? = null, yaw: Float = 0f, pitch: Float = 0f): Location {
            return Location(pos.x.toInt(), pos.y.toInt(), pos.z.toInt(), yaw, pitch, level
                    ?: ((pos as? Position)?.level))
        }
    }

    /**
     * Return a Location instance
     *
     * @return Location
     */
    fun asLocation(): Location {
        return Location(this.x.toInt(), this.y.toInt(), this.z.toInt(), this.yaw, this.pitch, this.level)
    }

    override fun toString(): String = "Location (level=${if (isValid()) level?.displayName else "null"}, x=x, y=y, z=z, yaw=yaw, pitch=pitch)"

    override fun equals(other: Any?): Boolean {
        return if (other is Location) super.equals(other) && other.yaw == this.yaw && other.pitch == this.pitch
        else super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + yaw.hashCode()
        result = 31 * result + pitch.hashCode()
        return result
    }
}