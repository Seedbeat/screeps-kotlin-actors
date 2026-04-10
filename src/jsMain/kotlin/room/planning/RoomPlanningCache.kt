package room.planning

import room.enums.RoomStage

data class RoomPlanningCache(
    val updatedAt: Int,
    val stage: RoomStage,
    val controllerLevel: Int,
    val energyCapacityAvailable: Int,
    val sourceCount: Int,
    val totalSourceOpenSides: Int,
    val sustainableIncome: Int,
    val bufferedEnergy: Int,
    val spawnCount: Int,
    val extensionCount: Int,
    val towerCount: Int,
    val spawnEnergyDeficit: Int,
    val extensionEnergyDeficit: Int,
    val towerEnergyDeficit: Int,
    val hasStorage: Boolean,
    val hasTerminal: Boolean,
    val controllerTicksToDowngrade: Int,
    val constructionSiteCount: Int,
    val remainingConstructionWork: Int,
    val weightedRemainingConstructionWork: Int,
    val criticalConstructionSiteCount: Int,
    val criticalConstructionWork: Int,
    val economyConstructionSiteCount: Int,
    val economyConstructionWork: Int,
    val lowPriorityConstructionSiteCount: Int,
    val lowPriorityConstructionWork: Int
)
