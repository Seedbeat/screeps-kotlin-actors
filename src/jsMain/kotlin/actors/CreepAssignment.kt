package actors

import actors.assignments.CreepAssignmentPhase

sealed class CreepAssignment {
    abstract val roomName: String

    sealed class PhaseAssignment : CreepAssignment() {
        abstract val phase: CreepAssignmentPhase
        abstract fun withPhase(phase: CreepAssignmentPhase): PhaseAssignment
    }

    data class ControllerUpkeep(
        override val roomName: String,
        val controllerId: String,
        override val phase: CreepAssignmentPhase = CreepAssignmentPhase.HARVEST
    ) : PhaseAssignment() {
        override fun withPhase(phase: CreepAssignmentPhase) = copy(phase = phase)
    }

    data class Construction(
        override val roomName: String,
        val constructionSiteId: String,
        override val phase: CreepAssignmentPhase = CreepAssignmentPhase.HARVEST
    ) : PhaseAssignment() {
        override fun withPhase(phase: CreepAssignmentPhase) = copy(phase = phase)
    }

    data class EnergyTransfer(
        override val roomName: String,
        val targetId: String,
        override val phase: CreepAssignmentPhase = CreepAssignmentPhase.HARVEST
    ) : PhaseAssignment() {
        override fun withPhase(phase: CreepAssignmentPhase) = copy(phase = phase)
    }
}
