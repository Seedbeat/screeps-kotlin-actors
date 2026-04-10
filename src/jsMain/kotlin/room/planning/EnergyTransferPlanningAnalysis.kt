package room.planning

import screeps.api.structures.Structure

data class EnergyTransferPlanningAnalysis(
    val activeTargets: List<Structure>,
    val target: Structure?,
    val demand: TaskDemand
)
