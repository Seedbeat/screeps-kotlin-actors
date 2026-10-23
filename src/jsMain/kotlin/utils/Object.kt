@file:Suppress("NOTHING_TO_INLINE")

package utils

object Object {
    inline val plain: dynamic get() = js("{}")
    inline fun plain(init: dynamic.() -> Unit): dynamic {
        val obj = plain
        obj.init()
        return obj
    }

    inline fun dataDescriptor(
        value: dynamic,
        writable: Boolean = false,
        enumerable: Boolean = false,
        configurable: Boolean = false,
    ): dynamic = plain {
        this.enumerable = enumerable
        this.configurable = configurable
        this.writable = writable
        this.value = value
    }

    inline fun accessorDescriptor(
        get: dynamic = undefined,
        set: dynamic = undefined,
        enumerable: Boolean = false,
        configurable: Boolean = false,
    ): dynamic = plain {
        this.enumerable = enumerable
        this.configurable = configurable
        this.get = get
        this.set = set
    }

    inline fun defineGlobal(name: String, value: dynamic) {
        defineProperty(
            globalThis,
            name,
            dataDescriptor(value = value, enumerable = true, configurable = true)
        )
    }

    // Original JS specs

    inline val globalThis: dynamic get() = js("globalThis")

    inline val Object: dynamic get() = js("Object")

    inline fun assign(target: dynamic, source: dynamic): Unit =
        Object.assign(target, source)

    inline fun entries(obj: dynamic): Array<Array<dynamic>> = if (obj == null) emptyArray() else
        Object.entries(obj)

    inline fun keys(obj: dynamic): Array<String> = if (obj == null) emptyArray() else
        Object.keys(obj)

    inline fun values(obj: dynamic): Array<dynamic> = if (obj == null) emptyArray() else
        Object.values(obj)

    inline fun defineProperty(obj: dynamic, prop: dynamic, descriptor: dynamic): dynamic =
        Object.defineProperty(obj, prop, descriptor)

    inline fun defineProperties(target: dynamic, props: dynamic): dynamic =
        Object.defineProperties(target, props)
}
