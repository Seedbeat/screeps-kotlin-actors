package actors.memory.base

import screeps.api.MemoryMarker

abstract class ObjectMemoryNode<T>(
    parent: MemoryMarker,
    selfKey: String
) : MemoryNode(parent, selfKey) {
    abstract fun read(): T?
    abstract fun write(value: T)
}