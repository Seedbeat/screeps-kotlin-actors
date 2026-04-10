package memory.base

import memory.delegates.memoryNodeValue
import screeps.api.MemoryMarker

abstract class SealedMemoryNode<T : Any>(
    parent: MemoryMarker,
    selfKey: String
): ObjectMemoryNode<T>(parent, selfKey) {

    private var _type: String? by memoryNodeValue()

    protected inline fun <reified S : T> sealedNode(node: ObjectMemoryNode<S>): Pair<String, ObjectMemoryNode<out T>> =
        S::class.simpleName!! to node

    abstract val mapping: Map<String, ObjectMemoryNode<out T>>

    override fun read(): T? = mapping[_type]?.value

    override fun write(value: T) {
        val type = value::class.simpleName!!
        _type = type

        @Suppress("UNCHECKED_CAST")
        (mapping[type] as? ObjectMemoryNode<T>)!!.value = value
    }

    override fun clear() {
        mapping[_type]?.value = null
        _type = null
    }
}