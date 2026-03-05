package actors

import actors.base.IIntent
import actors.base.IntentPriority
import creep.enums.Role

sealed class RoomIntent : RoomCommand(), IIntent {
    data class EnsurePopulation(
        override val priority: IntentPriority,
        override val createdTick: Int,
        override val interruptible: Boolean,
        val spawnActorId: String,
        val role: Role,
        val targetCount: Int
    ) : RoomIntent() {
        override val intentId: String = "${this::class.simpleName}$spawnActorId:$role"
    }
}
