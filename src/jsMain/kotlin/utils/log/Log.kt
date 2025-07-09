package utils.log

import Settings
import invoke

object Log {
    fun debug(sender: String = "Root", vararg o: Any?) = call(sender, LogLevel.DEBUG, null, *o)
    fun info(sender: String = "Root", vararg o: Any?) = call(sender, LogLevel.INFO, null, *o)
    fun warn(sender: String = "Root", vararg o: Any?) = call(sender, LogLevel.WARN, null, *o)
    fun error(sender: String = "Root", vararg o: Any?) = call(sender, LogLevel.ERROR, null, *o)

    fun debug(sender: String = "Root", minLevel: LogLevel? = null, vararg o: Any?) = call(sender, LogLevel.DEBUG, minLevel, *o)
    fun info(sender: String = "Root", minLevel: LogLevel? = null, vararg o: Any?) = call(sender, LogLevel.INFO, minLevel, *o)
    fun warn(sender: String = "Root", minLevel: LogLevel? = null, vararg o: Any?) = call(sender, LogLevel.WARN, minLevel, *o)
    fun error(sender: String = "Root", minLevel: LogLevel? = null, vararg o: Any?) = call(sender, LogLevel.ERROR, minLevel, *o)

    private fun call(sender: String, level: LogLevel, minLevel: LogLevel?, vararg o: Any?) {
        if (level.priority < (minLevel ?: Settings.LoggingLevel()).priority)
            return

        console.log("${level.value}[${sender.padStart(20, ' ')}]:", *o)
    }
}