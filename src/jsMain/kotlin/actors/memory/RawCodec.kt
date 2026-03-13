package actors.memory

import actors.base.ICodec

class RawCodec<T : Any> : ICodec<T> {
    override fun serialize(data: T): dynamic = data
    override fun deserialize(raw: dynamic): T = raw.unsafeCast<T>()
}
