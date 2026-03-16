package actors.memory.base

import screeps.api.MemoryMarker

abstract class MemoryNode<T>(
    internal val parent: MemoryMarker,
    internal val key: String
) : MemoryMarker {
    abstract fun read(): T?
    abstract fun write(value: T)

    protected var node: dynamic?
        get() = parent.asDynamic()[key]
        set(value) { parent.asDynamic()[key] = value }

    internal fun readNodeValue(name: String): dynamic? {
        if (node == null)
            return null

        return node[name]
    }

    internal fun writeNodeValue(name: String, value: dynamic) {
        if (node == null)
            node = js("{}")

        node[name] = value
    }
}