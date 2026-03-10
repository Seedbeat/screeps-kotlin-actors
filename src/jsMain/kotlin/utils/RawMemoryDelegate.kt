package utils

import actors.base.ICodecRaw
import screeps.api.MemoryMarker
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class RawMemoryMappingDelegate<T>(
    protected val default: () -> T,
    protected val serializer: (T) -> dynamic,
    protected val deserializer: (dynamic) -> T
) : ReadWriteProperty<MemoryMarker, T> {

    override fun getValue(thisRef: dynamic, property: KProperty<*>): T {
        val value = thisRef[property.name]

        return if (value != null) {
            deserializer(value)
        } else {
            val defaultValue = default()
            thisRef[property.name] = serializer(defaultValue)
            defaultValue
        }
    }

    override fun setValue(thisRef: dynamic, property: KProperty<*>, value: T) {
        thisRef[property.name] = serializer(value)
    }
}

open class RawMemoryMappingDelegateNullable<T>(
    protected val serializer: (T) -> dynamic,
    protected val deserializer: (dynamic) -> T
) : ReadWriteProperty<MemoryMarker, T?> {

    override fun getValue(thisRef: dynamic, property: KProperty<*>): T? {
        val value = thisRef[property.name]

        return if (value != null) {
            deserializer(value)
        } else {
            thisRef[property.name] = null
            null
        }
    }

    override fun setValue(thisRef: dynamic, property: KProperty<*>, value: T?) {
        thisRef[property.name] = value?.let(serializer)
    }
}

fun <T : Any> rawMemory(
    serializer: (T) -> dynamic,
    deserializer: (dynamic) -> T,
    default: () -> T
): ReadWriteProperty<MemoryMarker, T> = RawMemoryMappingDelegate(default, serializer, deserializer)

fun <T : Any> rawMemory(
    serializer: (T) -> dynamic,
    deserializer: (dynamic) -> T
): ReadWriteProperty<MemoryMarker, T?> = RawMemoryMappingDelegateNullable(serializer, deserializer)

fun <T> rawMemory(
    codec: ICodecRaw<T>,
    default: () -> T
): ReadWriteProperty<MemoryMarker, T> = RawMemoryMappingDelegate(
    default,
    serializer = codec::serializeRaw,
    deserializer = codec::deserializeRaw
)

fun <T : Any> rawMemory(
    codec: ICodecRaw<T>
): ReadWriteProperty<MemoryMarker, T?> = RawMemoryMappingDelegateNullable(
    serializer = codec::serializeRaw,
    deserializer = codec::deserializeRaw
)