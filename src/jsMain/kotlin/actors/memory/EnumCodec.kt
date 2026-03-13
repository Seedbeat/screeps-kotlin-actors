package actors.memory

import actors.base.ICodec

class EnumCodec<T : Enum<T>>(
    private val values: Array<T>,
    private val default: () -> T
) : ICodec<T> {

    override fun serialize(data: T): dynamic = data.name

    override fun deserialize(raw: dynamic): T {
        val name = raw as? String
        return values.firstOrNull { it.name == name } ?: default()
    }
}

inline fun <reified T : Enum<T>> enumCodec(
    noinline default: () -> T
): EnumCodec<T> = EnumCodec(enumValues<T>(), default)
