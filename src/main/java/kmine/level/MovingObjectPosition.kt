package kmine.level

import kmine.math.Vector3

class MovingObjectPosition {
    /**
     * 0 = block, 1 = entity
     */
    var typeOfHit: Int = 0

    var blockX: Int = 0
    var blockY: Int = 0
    var blockZ: Int = 0

    /**
     * Which side was hit. If its -1 then it went the full length of the ray trace.
     * Bottom = 0, Top = 1, East = 2, West = 3, North = 4, South = 5.
     */
    var sideHit: Int = 0

    private lateinit var hitVector: Vector3

    private lateinit var entityHit: Entity

    companion object {
        fun fromBlock(x: Int, y: Int, z: Int, side: Int, hitVector: Vector3): MovingObjectPosition {
            val objectPosition = MovingObjectPosition()
            objectPosition.typeOfHit = 0
            objectPosition.blockX = x
            objectPosition.blockY = y
            objectPosition.blockZ = z
            objectPosition.hitVector = Vector3(hitVector.x, hitVector.y, hitVector.z)
            objectPosition.sideHit = side
            return objectPosition
        }

        fun fromEntity(entity: Entity): MovingObjectPosition {
            val objectPosition = MovingObjectPosition()
            objectPosition.typeOfHit = 1
            objectPosition.entityHit = entity
            objectPosition.hitVector = Vector3(entity.x, entity.y, entity.z)
            return objectPosition
        }
    }
}