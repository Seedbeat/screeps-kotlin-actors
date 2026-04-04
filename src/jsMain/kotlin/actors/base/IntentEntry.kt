package actors.base

data class IntentEntry<T : Intent>(
    val intent: T,
    val createdTick: Int,
    var lastAttemptedTick: Int? = null
) {
    companion object {
        const val AGING_STEP_TICKS = 3
    }

    fun selectionScore(currentTick: Int): Int {
        val waitedTicks = (currentTick - (lastAttemptedTick ?: createdTick)).coerceAtLeast(0)
        return intent.priority.baseWeight + waitedTicks / AGING_STEP_TICKS
    }
}