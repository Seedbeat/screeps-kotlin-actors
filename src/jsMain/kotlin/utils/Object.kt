@file:Suppress("NOTHING_TO_INLINE")

package utils

object Object {
    inline val plain: dynamic get() = js("{}")
    inline fun plain(init: dynamic.() -> Unit): dynamic {
        val obj = plain
        obj.init()
        return obj
    }

    inline val Object: dynamic get() = js("Object")

    inline fun assign(target: dynamic, source: dynamic): Unit =
        Object.assign(target, source)

    inline fun entries(obj: dynamic): Array<Array<dynamic>> = if (obj == null) emptyArray() else
        Object.entries(obj)

    inline fun keys(obj: dynamic): Array<String> = if (obj == null) emptyArray() else
        Object.keys(obj)

    inline fun values(obj: dynamic): Array<dynamic> = if (obj == null) emptyArray() else
        Object.values(obj)
}