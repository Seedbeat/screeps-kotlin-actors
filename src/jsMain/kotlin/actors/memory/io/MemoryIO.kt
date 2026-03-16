package actors.memory.io

import kotlin.reflect.KProperty

interface MemoryIO<T> {
    fun readMemory(thisRef: T, property: KProperty<*>): dynamic?
    fun writeMemory(thisRef: T, property: KProperty<*>, value: dynamic)
}