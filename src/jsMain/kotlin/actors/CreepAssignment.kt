package actors

import actors.assignments.ControllerUpkeepPhase

sealed class CreepAssignment {
    abstract val roomName: String

    data class ControllerUpkeep(
        override val roomName: String,
        val controllerId: String,
        val phase: ControllerUpkeepPhase = ControllerUpkeepPhase.HARVEST
    ) : CreepAssignment()
}
