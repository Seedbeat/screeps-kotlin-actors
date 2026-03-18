package utils

inline val plainObject: dynamic get() = js("{}")
inline fun plainObject(init: dynamic.() -> Unit): dynamic {
    val obj = plainObject
    obj.init()
    return obj
}