package memory.base

import screeps.api.MemoryMarker

open class MemoryNodeMap<V : MemoryNode>(
    parent: MemoryMarker,
    selfKey: String,
    private val factory: (MemoryMarker, String) -> V
) : MemoryNode(parent, selfKey) {

    operator fun contains(key: String): Boolean =
        readNodeValue(key) != null

    operator fun get(key: String): V? =
        if (key in this) factory(memoryMarker, key) else null

    fun getOrCreate(key: String): V =
        factory(memoryMarker, key)

    fun remove(key: String) {
        deleteNodeValue(key)
    }
}