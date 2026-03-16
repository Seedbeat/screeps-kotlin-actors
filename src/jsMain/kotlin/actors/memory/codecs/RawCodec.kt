package actors.memory.codecs

import actors.base.ICodec

class RawCodec<T> : ICodec<T> {
    override fun serialize(data: T): dynamic = data
    override fun deserialize(raw: dynamic): T = raw.unsafeCast<T>()
}