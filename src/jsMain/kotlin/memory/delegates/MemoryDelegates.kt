package memory.delegates

import actors.base.Codec
import actors.base.asNullable
import memory.codecs.RawCodec
import memory.codecs.enumCodec
import memory.io.MemoryIO
import memory.io.MemoryRawIO
import screeps.api.MemoryMarker

class MemoryValueDelegate<T>(
    default: T,
    codec: Codec<T>
) : CodecValueDelegate<MemoryMarker, T>(default, codec),
    MemoryIO<MemoryMarker> by MemoryRawIO

fun <T> memoryValue(
    default: () -> T
): MemoryValueDelegate<T> = MemoryValueDelegate(default(), codec = RawCodec())

fun <T> memoryValue(): MemoryValueDelegate<T?> =
    MemoryValueDelegate(null, RawCodec<T>().asNullable())

inline fun <reified T : Enum<T>> memoryEnum(
    default: () -> T
): MemoryValueDelegate<T> = MemoryValueDelegate(default(), enumCodec())

fun <T> memoryObject(
    codec: Codec<T>,
    default: () -> T
): MemoryValueDelegate<T> = MemoryValueDelegate(default(), codec)

fun <T> memoryObject(
    codec: Codec<T>
): MemoryValueDelegate<T?> = MemoryValueDelegate(null, codec.asNullable())

