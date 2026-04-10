package memory.io

import memory.base.MemoryNode
import kotlin.reflect.KProperty

object MemoryNodeIO : MemoryIO<MemoryNode> {
    override fun readMemory(thisRef: MemoryNode, property: KProperty<*>): dynamic? {
        return thisRef.readNodeValue(property.name)
    }

    override fun writeMemory(thisRef: MemoryNode, property: KProperty<*>, value: dynamic) {
        thisRef.writeNodeValue(property.name, value)
    }
}