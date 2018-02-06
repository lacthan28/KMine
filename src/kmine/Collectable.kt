package kmine

abstract class Collectable : Thread() {
    var isGarbage = false
        set(value) {
            field = true
        }
}