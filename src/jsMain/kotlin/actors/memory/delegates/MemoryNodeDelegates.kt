package actors.memory.delegates

import actors.base.Codec
import actors.memory.base.MemoryNode
import actors.memory.codecs.RawCodec
import actors.memory.codecs.enumCodec
import actors.memory.io.MemoryIO
import actors.memory.io.MemoryNodeIO
import screeps.api.MemoryMarker
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class MemoryNodeValueDelegate<T : Any>(
    default: T,
    codec: Codec<T>
) : CodecValueDelegate<MemoryNode<*>, T>(default, codec),
    MemoryIO<MemoryNode<*>> by MemoryNodeIO

class MemoryNodeDelegate<T : MemoryNode<*>>(
    private val factory: (MemoryMarker, String) -> T
) : ReadOnlyProperty<MemoryMarker, T> {

    override fun getValue(thisRef: MemoryMarker, property: KProperty<*>): T =
        factory(thisRef, property.name)
}

fun <T : MemoryNode<*>> memoryNode(
    factory: (MemoryMarker, String) -> T
) = MemoryNodeDelegate(factory)

fun <T : Any> memoryNodeValue(
    default: () -> T,
) = MemoryNodeValueDelegate(
    default = default(),
    codec = RawCodec()
)

inline fun <reified T : Enum<T>> memoryNodeEnum(
    noinline default: () -> T
) = MemoryNodeValueDelegate(
    default = default(),
    codec = enumCodec()
)


