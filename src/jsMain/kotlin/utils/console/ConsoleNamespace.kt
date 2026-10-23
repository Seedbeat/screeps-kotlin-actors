package utils.console

import utils.Object

internal class ConsoleNamespace private constructor(private val name: String) {
    private val descriptors = Object.plain

    fun <T> property(
        name: String,
        type: ConsoleType<T>,
        get: () -> T,
        set: (T) -> Unit,
    ) {
        val path = "${this.name}.$name"
        descriptors[name] = Object.plain {
            enumerable = true
            this.get = get
            this.set = { value: dynamic -> set(type.decode(value, path)) }
        }
    }

    fun namespace(
        name: String,
        configure: ConsoleNamespace.() -> Unit,
    ) {
        val child = ConsoleNamespace("${this.name}.$name").apply(configure).build()
        descriptors[name] = Object.plain {
            value = child
            enumerable = true
        }
    }

    private fun build(): dynamic = Object.Object.defineProperties(Object.plain, descriptors)

    companion object {
        fun install(name: String, configure: ConsoleNamespace.() -> Unit) {
            val instance = ConsoleNamespace(name).apply(configure).build()
            Object.Object.defineProperty(js("globalThis"), name, Object.plain {
                value = instance
                configurable = true
                enumerable = true
            })
        }
    }
}
