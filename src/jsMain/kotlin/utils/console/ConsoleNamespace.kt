package utils.console

import utils.Object

/** Builds JavaScript accessors without exposing descriptors to domain bindings. */
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

    companion object {
        fun install(name: String, configure: ConsoleNamespace.() -> Unit) {
            val namespace = ConsoleNamespace(name).apply(configure)
            val instance = Object.Object.defineProperties(Object.plain, namespace.descriptors)
            Object.Object.defineProperty(js("globalThis"), name, Object.plain {
                value = instance
                configurable = true
                enumerable = true
            })
        }
    }
}
