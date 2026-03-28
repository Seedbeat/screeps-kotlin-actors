package actors

import memory.stage
import screeps.api.*

object RoomPlanningAnalyzer {
    fun analyze(room: Room): RoomPlanningCache {
        val myStructures = room.find(FIND_MY_STRUCTURES)
        val constructionSites = room.find(FIND_MY_CONSTRUCTION_SITES)

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
            remainingConstructionWork = constructionSites.sumOf { site ->
                (site.progressTotal - site.progress).coerceAtLeast(0)
            }
        )
    }
}
