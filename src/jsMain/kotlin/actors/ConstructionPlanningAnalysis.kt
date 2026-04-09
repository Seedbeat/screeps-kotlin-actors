package actors

import screeps.api.ConstructionSite

data class ConstructionPlanningAnalysis(
    val activeSites: List<ConstructionSite>,
    val targetSite: ConstructionSite?,
    val demand: TaskDemand
)
