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

    data object PlanWorkforce : RoomIntent(
        priority = IntentPriority.HIGH,
        repeatable = true
    )

    companion object {
        val recurring = listOf(
            EnsureControllerSurvival,
            PlanWorkforce
        )
    }
}
