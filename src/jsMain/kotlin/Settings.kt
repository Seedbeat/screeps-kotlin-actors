import screeps.api.*
import screeps.utils.memory.memory
import screeps.utils.mutableRecordOf
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

sealed class Settings<T : Any>(val defaultValue: T) {

    // Defaults here
    data object LoggingLevel : Settings<LogLevel>(LogLevel.DEBUG)
    data object ShowCreepPath : Settings<Boolean>(true)
    data object ShowStatistics : Settings<Boolean>(true)

    companion object : ILogging by Logging(Settings::class.simpleName!!) {

        val GlobalMemory.settings: MutableRecord<String, Any> by memory { mutableRecordOf() }

        inline operator fun <reified T : Enum<T>> get(parameter: Settings<T>): T =
            getInternal(parameter) { enumValueOf<T>(it.unsafeCast<String>()) }

        inline operator fun <reified T : Any> get(parameter: Settings<T>): T =
            getInternal(parameter)

        fun <T : Any> getInternal(parameter: Settings<T>, deserialize: (Any) -> T = { it.unsafeCast<T>() }): T =
            Memory.settings[parameter.key]?.let(deserialize) ?: parameter.defaultValue

        private operator fun <T : Any> set(parameter: Settings<T>, value: T) {
            log.info("Update parameter: '$parameter' -> $value")

            Memory.settings[parameter.key] = when (value) {
                is Enum<*> -> value.name
                else -> value
            }
        }
    }

    val key = this.toString()
    fun exist() = Memory.settings[this.key] != null
    fun setDefault() {
        Settings[this] = defaultValue
    }
}

inline operator fun <reified T : Any> Settings<T>.invoke() : T = Settings[this]
inline operator fun <reified T : Enum<T>> Settings<T>.invoke() : T = Settings[this]