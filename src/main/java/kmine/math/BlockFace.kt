package kmine.math

import java.util.*
import java.util.function.Predicate

enum class BlockFace private constructor(
        /**
         * Ordering index for D-U-N-S-W-E
         */
        private val index: Int,
        /**
         * Index of the opposite BlockFace in the VALUES array
         */
        private val opposite: Int,
        /**
         * Ordering index for the HORIZONTALS field (S-W-N-E)
         */
        private val horizontalIndex: Int,
        /**
         * The name of this BlockFace (up, down, north, etc.)
         */
        private val bfName: String,

        private val axisDirection: AxisDirection,
        /**
         * Normalized vector that points in the direction of this BlockFace
         */
        private val unitVector: Vector3) {
    DOWN(0, 1, -1, "down", AxisDirection.NEGATIVE, Vector3(0.0, -1.0, 0.0)),
    UP(1, 0, -1, "up", AxisDirection.POSITIVE, Vector3(0.0, 1.0, 0.0)),
    NORTH(2, 3, 2, "north", AxisDirection.NEGATIVE, Vector3(0.0, 0.0, -1.0)),
    SOUTH(3, 2, 0, "south", AxisDirection.POSITIVE, Vector3(0.0, 0.0, 1.0)),
    WEST(4, 5, 1, "west", AxisDirection.NEGATIVE, Vector3(-1.0, 0.0, 0.0)),
    EAST(5, 4, 3, "east", AxisDirection.POSITIVE, Vector3(1.0, 0.0, 0.0));

    companion object {
        /**
         * All faces in D-U-N-S-W-E order
         */
        private val VALUES = Array<BlockFace?>(6) { null }

        /**
         * All faces with horizontal axis in order S-W-N-E
         */
        private val HORIZONTALS = Array<BlockFace?>(4) { null }

        init {
            DOWN.axis = Axis.Y
            UP.axis = Axis.Y
            NORTH.axis = Axis.Z
            SOUTH.axis = Axis.Z
            WEST.axis = Axis.X
            EAST.axis = Axis.X

            for (face in values()) {
                VALUES.toMutableList()[face.index] = face

                if (face.getAxis()?.isHorizontal!!) {
                    HORIZONTALS[face.horizontalIndex] = face
                }
            }
        }
    }


    private var axis: Axis? = null

    /**
     * Get a BlockFace by it's index (0-5). The order is D-U-N-S-W-E
     */
    fun fromIndex(index: Int): BlockFace {
        return VALUES[(index % VALUES.size).abs()]!!
    }

    /**
     * Get a BlockFace by it's horizontal index (0-3). The order is S-W-N-E
     */
    fun fromHorizontalIndex(index: Int): BlockFace {
        return HORIZONTALS[(index % HORIZONTALS.size).abs()]!!
    }

    /**
     * Get the BlockFace corresponding to the given angle (0-360). An angle of 0 is SOUTH, an angle of 90 would be WEST
     */
    fun fromHorizontalAngle(angle: Double): BlockFace {
        return fromHorizontalIndex((angle / 90.0 + 0.5).floor() and 3)
    }

    fun fromAxis(axisDirection: AxisDirection, axis: Axis): BlockFace {
        for (face in VALUES) {
            if (face?.getAxisDirection() == axisDirection && face.getAxis() == axis) {
                return face
            }
        }

        throw RuntimeException("Unable to get face from axis: $axisDirection $axis")
    }

    /**
     * Choose a random BlockFace using the given Random
     */
    fun random(rand: Random): BlockFace {
        return VALUES[rand.nextInt(VALUES.size)]!!
    }

    /**
     * Get the index of this BlockFace (0-5). The order is D-U-N-S-W-E
     */
    fun getIndex(): Int {
        return index
    }

    /**
     * Get the horizontal index of this BlockFace (0-3). The order is S-W-N-E
     */
    fun getHorizontalIndex(): Int {
        return horizontalIndex
    }

    /**
     * Get the angle of this BlockFace (0-360)
     */
    fun getHorizontalAngle(): Float {
        return ((horizontalIndex and 3) * 90).toFloat()
    }

    /**
     * Get the name of this BlockFace (up, down, north, etc.)
     */
    fun getName(): String {
        return bfName
    }

    /**
     * Get the Axis of this BlockFace
     */
    fun getAxis(): Axis? {
        return axis
    }

    /**
     * Get the AxisDirection of this BlockFace
     */
    fun getAxisDirection(): AxisDirection {
        return axisDirection
    }

    /**
     * Get the unit vector of this BlockFace
     */
    fun getUnitVector(): Vector3 {
        return unitVector
    }

    /**
     * Returns an offset that addresses the block in front of this BlockFace
     */
    fun getXOffset(): Int {
        return if (axis == Axis.X) axisDirection.offset else 0
    }

    /**
     * Returns an offset that addresses the block in front of this BlockFace
     */
    fun getYOffset(): Int {
        return if (axis == Axis.Y) axisDirection.offset else 0
    }

    /**
     * Returns an offset that addresses the block in front of this BlockFace
     */
    fun getZOffset(): Int {
        return if (axis == Axis.Z) axisDirection.offset else 0
    }

    /**
     * Get the opposite BlockFace (e.g. DOWN => UP)
     */
    fun getOpposite(): BlockFace {
        return fromIndex(opposite)
    }

    /**
     * Rotate this BlockFace around the Y axis clockwise (NORTH => EAST => SOUTH => WEST => NORTH)
     */
    fun rotateY(): BlockFace {
        return when (this) {
            NORTH -> EAST
            EAST -> SOUTH
            SOUTH -> WEST
            WEST -> NORTH
            else -> throw RuntimeException("Unable to get Y-rotated face of " + this)
        }
    }

    /**
     * Rotate this BlockFace around the Y axis counter-clockwise (NORTH => WEST => SOUTH => EAST => NORTH)
     */
    fun rotateYCCW(): BlockFace {
        return when (this) {
            NORTH -> WEST
            EAST -> NORTH
            SOUTH -> EAST
            WEST -> SOUTH
            else -> throw RuntimeException("Unable to get counter-clockwise Y-rotated face of " + this)
        }
    }

    override fun toString(): String {
        return name
    }

    enum class Axis(private val axisName: String) : Predicate<BlockFace> {
        X("x"),
        Y("y"),
        Z("z");

        var plane: Plane? = null
            private set

        val isVertical: Boolean
            get() = plane == Plane.VERTICAL

        val isHorizontal: Boolean
            get() = plane == Plane.HORIZONTAL

        override fun test(t: BlockFace): Boolean {
            return t.getAxis() == this
        }

        override fun toString(): String {
            return axisName
        }

        companion object {
            init {
                //Circular dependency
                X.plane = Plane.HORIZONTAL
                Y.plane = Plane.VERTICAL
                Z.plane = Plane.HORIZONTAL
            }
        }
    }

    enum class AxisDirection constructor(val offset: Int, private val description: String) {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        override fun toString(): String {
            return description
        }
    }

    enum class Plane : Predicate<BlockFace>, Iterable<BlockFace> {
        HORIZONTAL,
        VERTICAL;

        private lateinit var faces: Array<BlockFace>

        fun random(rand: MRandom): BlockFace { //todo Default Random?
            return faces[rand.nextBoundedInt(faces.size)]
        }

        override fun test(t: BlockFace): Boolean {
            return t.getAxis()!!.plane == this
        }

        override fun iterator(): Iterator<BlockFace> {
            return faces.iterator()
        }

        companion object {
            init {
                //Circular dependency
                HORIZONTAL.faces = arrayOf(NORTH, EAST, SOUTH, WEST)
                VERTICAL.faces = arrayOf(UP, DOWN)
            }
        }
    }
}