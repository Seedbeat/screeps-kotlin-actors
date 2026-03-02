package actors.base

enum class IntentPriority(val baseWeight: Int) {
    CRITICAL(300),
    HIGH(200),
    NORMAL(100),
    LOW(0)
}