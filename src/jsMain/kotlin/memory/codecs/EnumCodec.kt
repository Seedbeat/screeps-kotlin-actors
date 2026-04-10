package memory.codecs

import actors.base.Codec


inline fun <reified T : Enum<T>> enumCodec(): Codec<T> = object : Codec<T> {
    override fun serialize(data: T): dynamic = data.name
    override fun deserialize(raw: dynamic): T = enumValueOf<T>(raw)
}
