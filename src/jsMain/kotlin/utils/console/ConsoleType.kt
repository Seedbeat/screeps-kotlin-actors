package utils.console

/** Decodes console input completely before a setting's handler can change state. */
internal fun interface ConsoleType<T> {
    fun decode(value: dynamic, path: String): T

    companion object {
        val boolean = ConsoleType { value, path ->
            require(jsTypeOf(value) == "boolean") { "$path must be a Boolean" }
            value.unsafeCast<Boolean>()
        }

        val nullableStringArray = ConsoleType { value, path ->
            if (value == null) {
                null
            } else {
                require(js("Array").isArray(value)) { "$path must be an array of strings or null" }
                for (label in value.unsafeCast<Array<dynamic>>()) {
                    require(jsTypeOf(label) == "string") { "$path labels must be strings" }
                }
                value.unsafeCast<Array<String>>()
            }
        }
    }
}
