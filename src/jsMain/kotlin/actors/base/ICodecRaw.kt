package actors.base

interface ICodecRaw<T> {
    fun serialize(raw: T): dynamic
    fun deserialize(data: dynamic): T
}