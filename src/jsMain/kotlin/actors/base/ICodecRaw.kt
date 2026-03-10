package actors.base

interface ICodecRaw<T> {
    fun serializeRaw(data: T): dynamic
    fun deserializeRaw(raw: dynamic): T
}