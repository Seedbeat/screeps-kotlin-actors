package actors.memory.io

import screeps.api.MemoryMarker
import kotlin.reflect.KProperty

object MemoryRawIO : MemoryIO<MemoryMarker> {
    override fun readMemory(thisRef: MemoryMarker, property: KProperty<*>): dynamic? =
        thisRef.asDynamic()[property.name]

    override fun writeMemory(thisRef: MemoryMarker, property: KProperty<*>, value: dynamic) {
        thisRef.asDynamic()[property.name] = value
    }
}