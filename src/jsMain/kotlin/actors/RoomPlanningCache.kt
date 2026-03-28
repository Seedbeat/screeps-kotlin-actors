package actors

import room.RoomStage

data class RoomPlanningCache(
    val updatedAt: Int,
    val stage: RoomStage,
    val controllerLevel: Int,
    val energyCapacityAvailable: Int,
    val spawnCount: Int,
    val extensionCount: Int,
    val towerCount: Int,
    val hasStorage: Boolean,
    val hasTerminal: Boolean,
    val constructionSiteCount: Int,
    val remainingConstructionWork: Int
)
