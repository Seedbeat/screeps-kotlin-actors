package actors.memory.delegates

import actors.base.Codec
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class CodecValueDelegate<R, T : Any>(
    private val default: T,
    private val codec: Codec<T>
) : ReadWriteProperty<R, T>, MemoryIO<R> {

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        val value = readMemory(thisRef, property)
        return if (value != null) {
            codec.deserialize(value)
        } else {
            writeMemory(thisRef, property, codec.serialize(default))
            default
        }
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        writeMemory(thisRef, property, codec.serialize(value))
    }
}

abstract class NullableCodecValueDelegate<R, T>(
    private val codec: Codec<T>
) : ReadWriteProperty<R, T?>, MemoryIO<R> {

    override fun getValue(thisRef: R, property: KProperty<*>): T? {
        val value = readMemory(thisRef, property)
        return if (value != null) {
            codec.deserialize(value)
        } else {
            writeMemory(thisRef, property, null)
            null
        }
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T?) {
        writeMemory(thisRef, property, value?.let(codec::serialize))
    }
}