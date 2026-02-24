package actor

import actor.enums.StopReasonType
import screeps.api.Game

data class TickState(
    val maxSteps: Int,
    val cpuReserve: Double,
    var rounds: Int = 0,
    var steps: Int = 0,
    var flushedResponses: Int = 0,
    var deliveredMessages: Int = 0,
    var stopReason: StopReasonType = StopReasonType.NONE
) {
    fun hasWork() = flushedResponses > 0 || deliveredMessages > 0
    fun isStopped() = stopReason != StopReasonType.NONE

    fun increaseFlushedResponses() {
        flushedResponses++
        steps++
    }

    fun increaseDeliveredMessages() {
        deliveredMessages++
        steps++
    }

    fun checkIsStopped(): Boolean {
        if (steps >= maxSteps) {
            stopReason = StopReasonType.MAX_STEPS
            return true
        }

        if (isCpuBudgetExceeded(cpuReserve)) {
            stopReason = StopReasonType.CPU_RESERVE
            return true
        }

        return false
    }

    private fun isCpuBudgetExceeded(cpuReserve: Double): Boolean {
        if (cpuReserve <= 0.0) return false
        return (Game.cpu.limit - Game.cpu.getUsed()) <= cpuReserve
    }
}