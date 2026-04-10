package memory.delegates

import actors.base.Codec
import actors.base.asNullable
import memory.base.MemoryNode
import memory.base.ObjectMemoryNode
import memory.codecs.RawCodec
import memory.codecs.enumCodec
import memory.io.MemoryIO
import memory.io.MemoryNodeIO
import screeps.api.MemoryMarker
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MemoryNodeValueDelegate<T>(
    default: T,
    codec: Codec<T>
) : CodecValueDelegate<MemoryNode, T>(default, codec),
    MemoryIO<MemoryNode> by MemoryNodeIO

class MemoryNodeDelegate<T : MemoryNode>(
    private val factory: (MemoryMarker, String) -> T
) : ReadOnlyProperty<MemoryMarker, T> {

    override fun getValue(thisRef: MemoryMarker, property: KProperty<*>): T {
        return factory(thisRef.nodeParent(), property.name)
    }
}

class MemoryNodeObjectDelegate<T>(
    private val factory: (MemoryMarker, String) -> ObjectMemoryNode<T>
) : ReadWriteProperty<MemoryMarker, T?> {

    override fun getValue(thisRef: MemoryMarker, property: KProperty<*>): T? =
        factory(thisRef.nodeParent(), property.name).value

    override fun setValue(thisRef: MemoryMarker, property: KProperty<*>, value: T?) {
        factory(thisRef.nodeParent(), property.name).value = value
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun MemoryMarker.nodeParent(): MemoryMarker =
    if (this is MemoryNode) memoryMarker else this

fun <T : MemoryNode> memoryNode(
    factory: (MemoryMarker, String) -> T
): MemoryNodeDelegate<T> = MemoryNodeDelegate(factory)

fun <T> memoryNodeObject(
    factory: (MemoryMarker, String) -> ObjectMemoryNode<T>
): MemoryNodeObjectDelegate<T> = MemoryNodeObjectDelegate(factory)

fun <T : Any> memoryNodeValue(
    default: () -> T,
): MemoryNodeValueDelegate<T> = MemoryNodeValueDelegate(default = default(), codec = RawCodec())

fun <T> memoryNodeValue(): MemoryNodeValueDelegate<T?> =
    MemoryNodeValueDelegate(default = null, codec = RawCodec<T>().asNullable())

inline fun <reified T : Enum<T>> memoryNodeEnum(
    noinline default: () -> T
): MemoryNodeValueDelegate<T> = MemoryNodeValueDelegate(default = default(), codec = enumCodec())

inline fun <reified T : Enum<T>> memoryNodeEnum(): MemoryNodeValueDelegate<T?> =
    MemoryNodeValueDelegate(default = null, codec = enumCodec<T>().asNullable())

