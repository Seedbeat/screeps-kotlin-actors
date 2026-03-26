package actors.base

interface Codec<T> {
    fun serialize(data: T): dynamic
    fun deserialize(raw: dynamic): T
}

fun <T> Codec<T>.asNullable(): Codec<T?> = object : Codec<T?> {
    override fun serialize(data: T?): dynamic =
        if (data == null) null else this@asNullable.serialize(data)

    override fun deserialize(raw: dynamic): T? =
        if (raw == null) null else this@asNullable.deserialize(raw)
}