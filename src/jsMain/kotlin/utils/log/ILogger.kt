package utils.log

interface ILogger {
    fun debug(vararg o: Any?)
    fun info(vararg o: Any?)
    fun warn(vararg o: Any?)
    fun error(vararg o: Any?)
}