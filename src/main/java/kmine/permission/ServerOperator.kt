package kmine.permission

interface ServerOperator {
    fun isOp(): Boolean
    fun setOp(value: Boolean)
}