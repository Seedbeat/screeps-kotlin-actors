package actors.memory.delegates

import actors.memory.MemoryNode
import screeps.api.MemoryMarker
import kotlin.reflect.KProperty

interface MemoryIO<T> {
    fun readMemory(thisRef: T, property: KProperty<*>): dynamic?
    fun writeMemory(thisRef: T, property: KProperty<*>, value: dynamic)
}

object MemoryNodeIO : MemoryIO<MemoryNode<*>> {
    override fun readMemory(thisRef: MemoryNode<*>, property: KProperty<*>): dynamic? {
        return thisRef.readNodeValue(property.name)
    }

    override fun writeMemory(thisRef: MemoryNode<*>, property: KProperty<*>, value: dynamic) {
        thisRef.writeNodeValue(property.name, value)
    }
}

object MemoryRawIO : MemoryIO<MemoryMarker> {
    override fun readMemory(thisRef: MemoryMarker, property: KProperty<*>): dynamic? =
        thisRef.asDynamic()[property.name]

    override fun writeMemory(thisRef: MemoryMarker, property: KProperty<*>, value: dynamic) {
        thisRef.asDynamic()[property.name] = value
    }
}