package actors.memory.base

import screeps.api.MemoryMarker
import screeps.utils.unsafe.delete
import utils.Object

abstract class MemoryNode(
    internal val parent: MemoryMarker,
    internal val selfKey: String
) : MemoryMarker {

    internal var self: dynamic?
        get() = parent.asDynamic()[selfKey]
        set(value) {
            parent.asDynamic()[selfKey] = value
        }

    internal inline val keys: Array<String>
        get() = Object.keys(self)

    internal inline val size: Int
        get() = keys.size

    internal inline val values: Array<dynamic>
        get() = Object.values(self)

    internal val memoryMarker: MemoryMarker
        get() {
            ensureNode()
            return self.unsafeCast<MemoryMarker>()
        }

    internal fun readNodeValue(key: String): dynamic? {
        if (self == null)
            return null

        return self[key]
    }

    internal fun writeNodeValue(key: String, value: dynamic) {
        ensureNode()
        self[key] = value
    }

    internal fun deleteNodeValue(key: String) {
        if (self == null)
            return

        delete(self[key])
    }

    internal fun ensureNode() {
        if (self == null) {
            self = Object.plain
        }
    }
}