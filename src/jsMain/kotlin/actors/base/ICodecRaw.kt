package actors.base

interface ICodecRaw<T> {
    fun serialize(data: T): dynamic
    fun deserialize(data: dynamic): T
}