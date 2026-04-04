package actors.base

enum class IntentPriority(val baseWeight: Int) {
    CRITICAL(30),
    HIGH(20),
    NORMAL(10),
    LOW(0)
}