package actors

import screeps.api.ConstructionSite

data class ConstructionPlanningAnalysis(
    val targetSite: ConstructionSite?,
    val desiredThroughput: Int,
    val builderCountCap: Int,
    val targetSiteCapacity: Int,
    val targetSiteAssigned: Int
)