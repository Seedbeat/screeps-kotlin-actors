package actors

import actors.base.Intent
import actors.base.IntentPriority

sealed class RoomIntent : RoomCommand(), Intent {
    data class EnsureControllerSurvival(
        override val priority: IntentPriority,
        override val createdTick: Int,
        override val interruptible: Boolean
    ) : RoomIntent() {
        override val intentId: String = this::class.simpleName!!
    }

    data class EnsureConstruction(
        override val priority: IntentPriority,
        override val createdTick: Int,
        override val interruptible: Boolean
    ) : RoomIntent() {
        override val intentId: String = this::class.simpleName!!
    }
}
