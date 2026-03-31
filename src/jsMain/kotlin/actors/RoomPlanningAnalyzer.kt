package actors

import actors.heuristics.ConstructionSiteHeuristics
import memory.stage
import room.constructionSites
import room.structures
import screeps.api.Game
import screeps.api.Room

object RoomPlanningAnalyzer {
    fun analyze(room: Room): RoomPlanningCache {
        val constructionSites = room.constructionSites.my
        val remainingConstructionWork = constructionSites.sumOf { site -> site.progressTotal - site.progress }

        return RoomPlanningCache(
            updatedAt = Game.time,
            stage = room.memory.stage,
            controllerLevel = room.controller?.level ?: 0,
            energyCapacityAvailable = room.energyCapacityAvailable,
            spawnCount = room.structures.my.spawns.size,
            extensionCount = room.structures.my.extensions.size,
            towerCount = room.structures.my.towers.size,
            hasStorage = room.storage != null,
            hasTerminal = room.terminal != null,
            constructionSiteCount = constructionSites.size,
            remainingConstructionWork = remainingConstructionWork,
            weightedRemainingConstructionWork = ConstructionSiteHeuristics.weightedRemainingWork(constructionSites)
        )
    }
}
