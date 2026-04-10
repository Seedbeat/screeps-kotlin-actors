package creep

import creep.enums.CreepAssignmentPhase

sealed interface CreepAssignment {
    val roomName: String

    sealed interface PhaseAssignment : CreepAssignment {
        val phase: CreepAssignmentPhase
        fun withPhase(phase: CreepAssignmentPhase): PhaseAssignment
    }

    data class ControllerUpkeep(
        override val roomName: String,
        val controllerId: String,
        override val phase: CreepAssignmentPhase = CreepAssignmentPhase.HARVEST
    ) : PhaseAssignment {
        override fun withPhase(phase: CreepAssignmentPhase) = copy(phase = phase)
    }

    data class ControllerProgress(
        override val roomName: String,
        val controllerId: String,
        override val phase: CreepAssignmentPhase = CreepAssignmentPhase.HARVEST
    ) : PhaseAssignment {
        override fun withPhase(phase: CreepAssignmentPhase) = copy(phase = phase)
    }

    data class Construction(
        override val roomName: String,
        val constructionSiteId: String,
        override val phase: CreepAssignmentPhase = CreepAssignmentPhase.HARVEST
    ) : PhaseAssignment {
        override fun withPhase(phase: CreepAssignmentPhase) = copy(phase = phase)
    }

    data class EnergyTransfer(
        override val roomName: String,
        val targetId: String,
        val goal: Goal,
        override val phase: CreepAssignmentPhase = CreepAssignmentPhase.HARVEST
    ) : PhaseAssignment {
        override fun withPhase(phase: CreepAssignmentPhase) = copy(phase = phase)

        sealed interface Goal {
            data object UntilFull : Goal
            data class Amount(val amount: Int) : Goal
            data class Percent(val percentage: Int) : Goal
        }
    }
}
