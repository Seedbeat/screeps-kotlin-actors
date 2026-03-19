package actors.memory.base

import actors.base.Codec
import screeps.api.MemoryMarker

open class MemoryMap<V>(
    parent: MemoryMarker,
    key: String,
    private val codec: Codec<V>
) : MemoryNode(parent, key) {

    operator fun contains(key: String): Boolean = readNodeValue(key) != null

    operator fun get(key: String): V? {
        val value = readNodeValue(key)
            ?: return null

        return codec.deserialize(value)
    }

    operator fun set(key: String, value: V) =
        writeNodeValue(key, codec.serialize(value))

    fun remove(key: String) = deleteNodeValue(key)
}