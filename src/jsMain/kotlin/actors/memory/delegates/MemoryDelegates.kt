package actors.memory.delegates

import actors.base.Codec
import actors.memory.codecs.RawCodec
import actors.memory.codecs.enumCodec
import actors.memory.io.MemoryIO
import actors.memory.io.MemoryRawIO
import screeps.api.MemoryMarker

class MemoryValueDelegate<T : Any>(
    default: T,
    codec: Codec<T>
) : CodecValueDelegate<MemoryMarker, T>(default, codec),
    MemoryIO<MemoryMarker> by MemoryRawIO

class MemoryNullableValueDelegate<T>(
    codec: Codec<T>
) : NullableCodecValueDelegate<MemoryMarker, T>(codec),
    MemoryIO<MemoryMarker> by MemoryRawIO

fun <T : Any> memoryValue(
    default: () -> T
) = MemoryValueDelegate(default(), RawCodec())

inline fun <reified T : Enum<T>> memoryEnum(
    default: () -> T
) = MemoryValueDelegate(default(), enumCodec())

fun <T : Any> memoryObject(
    codec: Codec<T>,
    default: () -> T
) = MemoryValueDelegate(default(), codec)

fun <T : Any> memoryObject(
    codec: Codec<T>
) = MemoryNullableValueDelegate(codec)

