package kmine.math

import kotlin.math.*

class Vector2(var x: Double = 0.0, var y: Double = 0.0) {
    val floorX = this.x.toInt()
    val floorY = this.y.toInt()

    fun add(x: Vector2, y: Double = 0.0) = add(x.x, x.y)

    fun add(x: Double, y: Double = 0.0) = Vector2(this.x + x, this.y + y)

    fun subtract(x: Vector2, y: Double = 0.0) = add(-x.x, -x.y)

    fun subtract(x: Double, y: Double = 0.0) = add(-x, -y)

    fun ceil() = Vector2(ceil(this.x), ceil(this.y))

    fun floor() = Vector2(floorX.toDouble(), floorY.toDouble())

    fun round() = Vector2(round(this.x), round(this.y))

    fun abs() = Vector2(abs(this.x), abs(this.y))

    fun multiply(number: Double) = Vector2(this.x * number, this.y * number)

    fun divide(number: Double) = Vector2(this.x / number, this.y / number)

    fun distance(x: Vector2, y: Double = 0.0) = sqrt(distanceSquared(x.x, x.y))

    fun distance(x: Double, y: Double = 0.0) = sqrt(distanceSquared(x, y))

    fun distanceSquared(x: Vector2, y: Double = 0.0) = this.distanceSquared(x.x, x.y)

    fun distanceSquared(x: Double, y: Double = 0.0) = ((this.x - x).pow(2)) + ((this.y - y).pow(2))

    fun length() = sqrt(lengthSquared())

    fun lengthSquared() = this.x * this.x + this.y * this.y

    fun normalize(): Vector2 {
        val len = lengthSquared()
        return if (len != 0.0) {
            divide(sqrt(len))
        } else Vector2()
    }

    fun dot(vector: Vector2) = this.x * vector.x + this.y * vector.y

    override fun toString(): String {
        return "Vector2(x= $x, y= $y)"
    }
}