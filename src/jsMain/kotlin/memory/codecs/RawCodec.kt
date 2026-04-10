package memory.codecs

import actors.base.Codec

class RawCodec<T> : Codec<T> {
    override fun serialize(data: T): dynamic = data
    override fun deserialize(raw: dynamic): T = raw.unsafeCast<T>()
}