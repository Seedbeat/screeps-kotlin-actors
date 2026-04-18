package room.planning

import heuristics.ConstructionSiteHeuristics
import map.openSidesCount
import memory.stage
import room.find
import screeps.api.FIND_SOURCES
import screeps.api.Game
import screeps.api.Room
import store.energyStore

object RoomPlanningAnalyzer {
    fun analyze(room: Room): RoomPlanningCache {
        val constructionSites = room.find.my.constructionSites
        val remainingConstructionWork = constructionSites.sumOf { site -> site.progressTotal - site.progress }
        val terrain = room.getTerrain()
        val sources = room.find(FIND_SOURCES)
        val totalSourceOpenSides = sources.sumOf { source -> source.openSidesCount(terrain) }

        val criticalSites = constructionSites.filter { site -> ConstructionSiteHeuristics.sitePriority(site) <= 1 }
        val economySites = constructionSites.filter { site ->
            ConstructionSiteHeuristics.sitePriority(site) == 2
        }
        val lowPrioritySites = constructionSites.filter { site ->
            ConstructionSiteHeuristics.sitePriority(site) >= 3
        }

        val containersEnergy = room.find.my.structures.containers.sumOf { container ->
            container.energyStore.used
        }

        val storageEnergy = room.storage?.energyStore?.used ?: 0
        val terminalEnergy = room.terminal?.energyStore?.used ?: 0
        val bufferedEnergy = room.energyAvailable + containersEnergy + storageEnergy + terminalEnergy

        return RoomPlanningCache(
            updatedAt = Game.time,
            stage = room.memory.stage,
            controllerLevel = room.controller?.level ?: 0,
            energyCapacityAvailable = room.energyCapacityAvailable,
            sourceCount = sources.size,
            totalSourceOpenSides = totalSourceOpenSides,
            sustainableIncome = sources.size * 10,
            bufferedEnergy = bufferedEnergy,
            spawnCount = room.find.my.structures.spawns.size,
            extensionCount = room.find.my.structures.extensions.size,
            towerCount = room.find.my.structures.towers.size,
            spawnEnergyDeficit = room.find.my.structures.spawns.sumOf { spawn -> spawn.energyStore.free },
            extensionEnergyDeficit = room.find.my.structures.extensions.sumOf { extension -> extension.energyStore.free },
            towerEnergyDeficit = room.find.my.structures.towers.sumOf { tower -> tower.energyStore.free },
            hasStorage = room.storage != null,
            hasTerminal = room.terminal != null,
            controllerTicksToDowngrade = room.controller?.ticksToDowngrade ?: Int.MAX_VALUE,
            constructionSiteCount = constructionSites.size,
            remainingConstructionWork = remainingConstructionWork,
            weightedRemainingConstructionWork = ConstructionSiteHeuristics.weightedRemainingWork(constructionSites),
            criticalConstructionSiteCount = criticalSites.size,
            criticalConstructionWork = criticalSites.sumOf { site -> site.progressTotal - site.progress },
            economyConstructionSiteCount = economySites.size,
            economyConstructionWork = economySites.sumOf { site -> site.progressTotal - site.progress },
            lowPriorityConstructionSiteCount = lowPrioritySites.size,
            lowPriorityConstructionWork = lowPrioritySites.sumOf { site -> site.progressTotal - site.progress }
        )
    }
}
