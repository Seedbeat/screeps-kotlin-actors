package memory.delegates

import actors.base.Codec
import memory.io.MemoryIO
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class CodecValueDelegate<R, T>(
    private val default: T,
    private val codec: Codec<T>
) : ReadWriteProperty<R, T>, MemoryIO<R> {

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        val raw = readMemory(thisRef, property)
        return if (raw != null) {
            codec.deserialize(raw)
        } else {
            writeMemory(thisRef, property, codec.serialize(default))
            default
        }
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        writeMemory(thisRef, property, codec.serialize(value))
    }
}