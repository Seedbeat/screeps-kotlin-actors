package actors.memory.codecs

import actors.base.ICodec


inline fun <reified T : Enum<T>> enumCodec(): ICodec<T> = object : ICodec<T> {
    override fun serialize(data: T): dynamic = data.name
    override fun deserialize(raw: dynamic): T = enumValueOf<T>(raw)
}
