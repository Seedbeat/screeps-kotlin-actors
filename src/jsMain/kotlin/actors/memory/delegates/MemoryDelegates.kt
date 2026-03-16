package actors.memory.delegates

import actors.base.ICodec
import actors.memory.codecs.RawCodec
import actors.memory.codecs.enumCodec
import screeps.api.MemoryMarker

class MemoryValueDelegate<T : Any>(
    default: T,
    codec: ICodec<T>
) : CodecValueDelegate<MemoryMarker, T>(default, codec),
    MemoryIO<MemoryMarker> by MemoryRawIO

class MemoryNullableValueDelegate<T>(
    codec: ICodec<T>
) : NullableCodecValueDelegate<MemoryMarker, T>(codec),
    MemoryIO<MemoryMarker> by MemoryRawIO

fun <T : Any> memoryValue(
    default: () -> T
) = MemoryValueDelegate(default(), RawCodec())

inline fun <reified T : Enum<T>> memoryEnum(
    default: () -> T
) = MemoryValueDelegate(default(), enumCodec())

fun <T : Any> memoryObject(
    codec: ICodec<T>,
    default: () -> T
) = MemoryValueDelegate(default(), codec)

fun <T : Any> memoryObject(
    codec: ICodec<T>
) = MemoryNullableValueDelegate(codec)

