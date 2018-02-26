package kmine.math

class Vector2f(val x: Float = 0f, val y: Float = 0f) {

    fun getFloorX(): Int {
        return this.x.floor()
    }

    fun getFloorY(): Int {
        return this.y.floor()
    }

    fun add(x: Float): Vector2f {
        return this.add(x, 0f)
    }

    fun add(x: Float, y: Float): Vector2f {
        return Vector2f(this.x + x, this.y + y)
    }

    fun add(x: Vector2f): Vector2f {
        return this.add(x.x, x.y)
    }

    fun subtract(x: Float): Vector2f {
        return this.subtract(x, 0f)
    }

    fun subtract(x: Float, y: Float): Vector2f {
        return this.add(-x, -y)
    }

    fun subtract(x: Vector2f): Vector2f {
        return this.add(-x.x, -x.y)
    }

    fun ceil(): Vector2f {
        return Vector2f((this.x + 1).toInt().toFloat(), (this.y + 1).toInt().toFloat())
    }

    fun floor(): Vector2f {
        return Vector2f(this.getFloorX().toFloat(), this.getFloorY().toFloat())
    }

    fun round(): Vector2f {
        return Vector2f(Math.round(this.x).toFloat(), Math.round(this.y).toFloat())
    }

    fun abs(): Vector2f {
        return Vector2f(Math.abs(this.x), Math.abs(this.y))
    }

    fun multiply(number: Float): Vector2f {
        return Vector2f(this.x * number, this.y * number)
    }

    fun divide(number: Float): Vector2f {
        return Vector2f(this.x / number, this.y / number)
    }

    fun distance(x: Float): Double {
        return this.distance(x, 0f)
    }

    fun distance(x: Float, y: Float): Double {
        return Math.sqrt(this.distanceSquared(x, y))
    }

    fun distance(vector: Vector2f): Double {
        return Math.sqrt(this.distanceSquared(vector.x, vector.y))
    }

    fun distanceSquared(x: Float): Double {
        return this.distanceSquared(x, 0f)
    }

    fun distanceSquared(x: Float, y: Float): Double {
        return Math.pow((this.x - x).toDouble(), 2.0) + Math.pow((this.y - y).toDouble(), 2.0)
    }

    fun distanceSquared(vector: Vector2f): Double {
        return this.distanceSquared(vector.x, vector.y)
    }

    fun length(): Double {
        return Math.sqrt(this.lengthSquared().toDouble())
    }

    fun lengthSquared(): Float {
        return this.x * this.x + this.y * this.y
    }

    fun normalize(): Vector2f {
        val len = this.lengthSquared()
        return if (len != 0f) {
            this.divide(Math.sqrt(len.toDouble()).toFloat())
        } else Vector2f(0f, 0f)
    }

    fun dot(v: Vector2f): Float {
        return this.x * v.x + this.y * v.y
    }

    override fun toString(): String {
        return "Vector2(x=" + this.x + ",y=" + this.y + ")"
    }
}