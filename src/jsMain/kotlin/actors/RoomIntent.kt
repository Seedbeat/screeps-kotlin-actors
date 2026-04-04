package actors

import actors.base.Intent
import actors.base.IntentPriority

sealed class RoomIntent(
    override val priority: IntentPriority,
    override val repeatable: Boolean
) : RoomCommand, Intent {
    override val intentId: String = this::class.simpleName!!

    data object EnsureControllerSurvival : RoomIntent(
        priority = IntentPriority.CRITICAL,
        repeatable = true
    )

    data object EnsureConstruction : RoomIntent(
        priority = IntentPriority.NORMAL,
        repeatable = true
    )

    data object EnsureEnergyTransfer : RoomIntent(
        priority = IntentPriority.HIGH,
        repeatable = true
    )

    companion object {
        val recurring = listOf(
            EnsureControllerSurvival,
            EnsureConstruction,
            EnsureEnergyTransfer
        )
    }
}
