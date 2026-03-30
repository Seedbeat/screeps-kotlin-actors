package actors.memory.base

import screeps.api.MemoryMarker

abstract class EmptyMemoryNode<T : Any>(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<T>(parent, selfKey) {
    override fun write(value: T) = Unit
    override fun clear() = Unit
}