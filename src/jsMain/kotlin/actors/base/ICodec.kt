package actors.base

interface ICodec<T> {
    fun serialize(data: T): dynamic
    fun deserialize(raw: dynamic): T
}