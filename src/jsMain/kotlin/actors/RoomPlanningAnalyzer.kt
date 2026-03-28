package actors

import actors.heuristics.ConstructionSiteHeuristics
import memory.stage
import screeps.api.*

object RoomPlanningAnalyzer {
    fun analyze(room: Room): RoomPlanningCache {
        val myStructures = room.find(FIND_MY_STRUCTURES)
        val constructionSites = room.find(FIND_MY_CONSTRUCTION_SITES)
        val remainingConstructionWork = constructionSites.sumOf { site -> site.progressTotal - site.progress }

        return RoomPlanningCache(
            updatedAt = Game.time,
            stage = room.memory.stage,
            controllerLevel = room.controller?.level ?: 0,
            energyCapacityAvailable = room.energyCapacityAvailable,
            spawnCount = myStructures.count { it.structureType == STRUCTURE_SPAWN },
            extensionCount = myStructures.count { it.structureType == STRUCTURE_EXTENSION },
            towerCount = myStructures.count { it.structureType == STRUCTURE_TOWER },
            hasStorage = room.storage != null,
            hasTerminal = myStructures.any { it.structureType == STRUCTURE_TERMINAL },
            constructionSiteCount = constructionSites.size,
            remainingConstructionWork = remainingConstructionWork,
            weightedRemainingConstructionWork = ConstructionSiteHeuristics.weightedRemainingWork(constructionSites)
        )
    }
}
