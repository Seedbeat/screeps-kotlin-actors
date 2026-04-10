package memory.base

import screeps.api.MemoryMarker

abstract class ObjectMemoryNode<T>(
    parent: MemoryMarker,
    selfKey: String
) : MemoryNode(parent, selfKey) {

    var value: T?
        get() = read()
        set(value) = if (value == null) clear() else write(value)

    protected abstract fun read(): T?
    protected abstract fun write(value: T)
    protected abstract fun clear()
}