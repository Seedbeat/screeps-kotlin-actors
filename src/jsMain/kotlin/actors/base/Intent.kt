package actors.base

import actor.message.Command

interface Intent : Command {
    val intentId: String
    val priority: IntentPriority
    val createdTick: Int
    val interruptible: Boolean

    fun effectivePriorityScore(currentTick: Int, agingStepTicks: Int): Int {
        val waitedTicks = (currentTick - createdTick).coerceAtLeast(0)
        val agingBonus = waitedTicks / agingStepTicks.coerceAtLeast(1)
        return priority.baseWeight + agingBonus
    }
}