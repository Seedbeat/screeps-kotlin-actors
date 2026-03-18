package actors.memory.base

import screeps.api.MemoryMarker
import screeps.api.MutableRecord
import screeps.utils.unsafe.delete
import utils.plainObject

abstract class MemoryNode(
    internal val parent: MemoryMarker,
    internal val key: String
) : MemoryMarker {

    internal var node: dynamic?
        get() = parent.asDynamic()[key]
        set(value) { parent.asDynamic()[key] = value }

    internal val record: MutableRecord<String, Any?>?
        get() = node?.unsafeCast<MutableRecord<String, dynamic?>>()

    internal val memoryMarker: MemoryMarker
        get() {
            ensureNode()
            return node.unsafeCast<MemoryMarker>()
        }

    internal fun readNodeValue(name: String): dynamic? {
        if (node == null)
            return null

        return node[name]
    }

    internal fun writeNodeValue(name: String, value: dynamic) {
        ensureNode()
        node[name] = value
    }

    internal fun deleteNodeValue(name: String) {
        if (node == null)
            return

        delete(node[name])
    }

    internal fun ensureNode() {
        if (node == null) {
            node = plainObject
        }
    }
}