package actors.memory.base

import screeps.api.MemoryMarker

abstract class ObjectMemoryNode<T>(
    parent: MemoryMarker,
    key: String
) : MemoryNode(parent, key) {

    abstract fun read(): T?
    abstract fun write(value: T)
}