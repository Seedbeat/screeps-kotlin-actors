package utils.console

import utils.Object

internal class ConsoleNamespace private constructor(private val name: String) {
    private val descriptors = Object.plain

    fun <T> action(name: String, run: () -> T) {
        descriptors[name] = Object.accessorDescriptor(get = run)
    }

    fun <T> property(
        name: String,
        type: ConsoleType<T>,
        get: () -> T,
        set: (T) -> Unit,
    ) {
        val path = "${this.name}.$name"
        descriptors[name] = Object.accessorDescriptor(
            enumerable = true,
            get = get,
            set = { value: dynamic -> set(type.decode(value, path)) }
        )
    }

    fun namespace(
        name: String,
        configure: ConsoleNamespace.() -> Unit,
    ) {
        val child = ConsoleNamespace("${this.name}.$name").apply(configure).build()
        descriptors[name] = Object.dataDescriptor(value = child, enumerable = true)
    }

    private fun build(): dynamic = Object.defineProperties(Object.plain, descriptors)

    companion object {
        fun install(name: String, configure: ConsoleNamespace.() -> Unit) {
            Object.defineGlobal(name, ConsoleNamespace(name).apply(configure).build())
        }
    }
}
