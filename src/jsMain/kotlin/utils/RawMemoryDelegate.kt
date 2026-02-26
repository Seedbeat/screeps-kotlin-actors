package utils

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

fun <T : Any> rawMemoryWithSerializer(
    default: () -> T,
    serializer: (T) -> dynamic,
    deserializer: (dynamic) -> T
): ReadWriteProperty<MemoryMarker, T> = RawMemoryMappingDelegate(default, serializer, deserializer)

fun <T : Any> rawMemoryWithSerializer(
    serializer: (T) -> dynamic,
    deserializer: (dynamic) -> T
): ReadWriteProperty<MemoryMarker, T?> = RawMemoryMappingDelegateNullable(serializer, deserializer)