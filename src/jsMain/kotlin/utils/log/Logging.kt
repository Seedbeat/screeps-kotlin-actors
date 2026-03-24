package utils.log

class Logging(sender: String, minLevel: LogLevel? = null) : ILogging {

    override val log: ILogger = object : ILogger {
        private val sender: String = sender.padStart(45, ' ')
        override fun debug(vararg o: Any?) = Log.debug(this.sender, minLevel, *o)
        override fun info(vararg o: Any?) = Log.info(this.sender, minLevel, *o)
        override fun warn(vararg o: Any?) = Log.warn(this.sender, minLevel, *o)
        override fun error(vararg o: Any?) = Log.error(this.sender, minLevel, *o)
    }

    companion object {
        inline operator fun <reified T : Any> invoke(minLevel: LogLevel? = null): Logging {
            return Logging(T::class.simpleName!!, minLevel)
        }

        inline operator fun <reified T : Any> invoke(name: String, minLevel: LogLevel? = null): Logging {
            return Logging("${T::class.simpleName!!}:$name", minLevel)
        }
    }
}