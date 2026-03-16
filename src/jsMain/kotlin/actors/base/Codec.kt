package actors.base

interface Codec<T> {
    fun serialize(data: T): dynamic
    fun deserialize(raw: dynamic): T
}