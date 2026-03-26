package actors

import actors.assignments.CreepAssignmentPhase

sealed class CreepAssignment {
    abstract val roomName: String

    data class ControllerUpkeep(
        override val roomName: String,
        val controllerId: String,
        val phase: CreepAssignmentPhase = CreepAssignmentPhase.HARVEST
    ) : CreepAssignment()

    data class Construction(
        override val roomName: String,
        val constructionSiteId: String,
        val phase: CreepAssignmentPhase = CreepAssignmentPhase.HARVEST
    ) : CreepAssignment()
}
