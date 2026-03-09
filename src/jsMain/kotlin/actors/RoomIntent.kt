package actors

import actors.base.IIntent
import actors.base.IntentPriority

sealed class RoomIntent : RoomCommand(), IIntent {
    data class EnsureControllerSurvival(
        override val priority: IntentPriority,
        override val createdTick: Int,
        override val interruptible: Boolean
    ) : RoomIntent() {
        override val intentId: String = this::class.simpleName!!
    }
}
