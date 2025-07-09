package utils.log

enum class LogLevel(val value: String, val priority: Int) {
    DEBUG("[DEBUG]", 0),
    INFO(" [INFO]", 1),
    WARN(" [WARN]", 2),
    ERROR("[ERROR]", 3)
}