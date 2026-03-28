package actors

import creep.BodyRecipe
import memory.stage
import room.RoomStage
import screeps.api.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

object ConstructionPlanningPolicy {
    fun analyze(
        room: Room,
        planningCache: RoomPlanningCache?,
        constructionSites: Array<ConstructionSite>,
        assignedSiteCounts: Map<String, Int>
    ): ConstructionPlanningAnalysis {
        val targetSite = selectTargetSite(room, constructionSites, assignedSiteCounts)
            ?: return ConstructionPlanningAnalysis(
                targetSite = null,
                desiredThroughput = 0,
                builderCountCap = maxBuildersCap(planningCache?.stage ?: room.memory.stage),
                targetSiteCapacity = 0,
                targetSiteAssigned = 0
            )

        val targetSiteCapacity = constructionSiteCapacity(targetSite)
        val targetSiteAssigned = assignedSiteCounts[targetSite.id] ?: 0

        return ConstructionPlanningAnalysis(
            targetSite = targetSite,
            desiredThroughput = desiredThroughput(room, planningCache, constructionSites, targetSite),
            builderCountCap = maxBuildersCap(planningCache?.stage ?: room.memory.stage),
            targetSiteCapacity = targetSiteCapacity,
            targetSiteAssigned = targetSiteAssigned
        )
    }

    fun spawnableThroughput(room: Room, targetSite: ConstructionSite): Int =
        constructionThroughput(room, targetSite, room.energyAvailable)

    private fun desiredThroughput(
        room: Room,
        planningCache: RoomPlanningCache?,
        constructionSites: Array<ConstructionSite>,
        primarySite: ConstructionSite
    ): Int {
        val remainingWork = weightedRemainingConstructionWork(planningCache, constructionSites)
        if (remainingWork <= 0) {
            return 0
        }

        val targetTicks = constructionTargetTicks(room, planningCache, primarySite)
        val requiredThroughput = ceil(remainingWork.toDouble() / targetTicks).toInt().coerceAtLeast(BUILD_POWER)
        val maxThroughput = maxThroughputCap(room, planningCache, primarySite)
        return requiredThroughput.coerceAtMost(maxThroughput)
    }

    private fun weightedRemainingConstructionWork(
        planningCache: RoomPlanningCache?,
        constructionSites: Array<ConstructionSite>
    ): Int {
        if (planningCache != null && planningCache.constructionSiteCount == constructionSites.size) {
            val liveWeighted = constructionSites.sumOf { site ->
                ((site.progressTotal - site.progress).coerceAtLeast(0) * constructionPriorityWeight(site)).roundToInt()
            }

            val liveRaw = constructionSites.sumOf { site ->
                (site.progressTotal - site.progress).coerceAtLeast(0)
            }

            if (liveRaw == planningCache.remainingConstructionWork) {
                return liveWeighted
            }
        }

        return constructionSites.sumOf { site ->
            ((site.progressTotal - site.progress).coerceAtLeast(0) * constructionPriorityWeight(site)).roundToInt()
        }
    }

    private fun constructionTargetTicks(
        room: Room,
        planningCache: RoomPlanningCache?,
        primarySite: ConstructionSite
    ): Int {
        val baseTicks = when (planningCache?.stage ?: room.memory.stage) {
            RoomStage.Uninitialized,
            RoomStage.Stage1 -> 1800

            RoomStage.Stage2,
            RoomStage.Stage3 -> 1200

            RoomStage.Stage4,
            RoomStage.Stage5 -> 800

            RoomStage.Stage6,
            RoomStage.Stage7 -> 500

            RoomStage.Stage8,
            RoomStage.StageMax -> 300
        }

        val energyRatio = if (room.energyCapacityAvailable <= 0) {
            0.0
        } else {
            room.energyAvailable.toDouble() / room.energyCapacityAvailable.toDouble()
        }

        val energyModifier = when {
            energyRatio >= 0.9 -> 0.75
            energyRatio >= 0.6 -> 1.0
            energyRatio >= 0.3 -> 1.2
            else -> 1.5
        }

        val priorityModifier = when (constructionPriority(primarySite)) {
            0 -> 0.5
            1 -> 0.7
            2 -> 0.9
            3 -> 1.1
            4 -> 1.4
            else -> 1.0
        }

        return max(100, (baseTicks * energyModifier * priorityModifier).roundToInt())
    }

    private fun maxThroughputCap(
        room: Room,
        planningCache: RoomPlanningCache?,
        primarySite: ConstructionSite
    ): Int {
        val maxBuilders = maxBuildersCap(planningCache?.stage ?: room.memory.stage)
        val energyBudget = planningCache?.energyCapacityAvailable ?: room.energyCapacityAvailable
        val perBuilderThroughput = max(BUILD_POWER, constructionThroughput(room, primarySite, energyBudget))
        return maxBuilders * perBuilderThroughput
    }

    private fun maxBuildersCap(stage: RoomStage): Int = when (stage) {
        RoomStage.Uninitialized,
        RoomStage.Stage1 -> 1

        RoomStage.Stage2,
        RoomStage.Stage3 -> 2

        RoomStage.Stage4,
        RoomStage.Stage5 -> 3

        RoomStage.Stage6,
        RoomStage.Stage7,
        RoomStage.Stage8,
        RoomStage.StageMax -> 4
    }

    private fun constructionThroughput(room: Room, targetSite: ConstructionSite, energyBudget: Int): Int {
        val sampleAssignment = CreepAssignment.Construction(
            roomName = room.name,
            constructionSiteId = targetSite.id
        )
        val body = BodyRecipe.selectBodySpecByAssignment(energyBudget, sampleAssignment)
            ?: return 0

        return body.body.count { part -> part == WORK } * BUILD_POWER
    }

    private fun selectTargetSite(
        room: Room,
        constructionSites: Array<ConstructionSite>,
        assignedSiteCounts: Map<String, Int>
    ): ConstructionSite? {
        if (constructionSites.isEmpty()) {
            return null
        }

        return constructionSites
            .filter { site -> (assignedSiteCounts[site.id] ?: 0) < constructionSiteCapacity(site) }
            .sortedWith(
                compareBy<ConstructionSite> { constructionPriority(it) }
                    .thenBy { assignedSiteCounts[it.id] ?: 0 }
                    .thenBy { room.controller?.pos?.getRangeTo(it.pos) ?: 0 }
                    .thenByDescending { it.progressTotal - it.progress }
            )
            .firstOrNull()
    }

    private fun constructionSiteCapacity(site: ConstructionSite): Int = when (site.structureType) {
        STRUCTURE_SPAWN,
        STRUCTURE_STORAGE,
        STRUCTURE_TERMINAL -> 2

        STRUCTURE_RAMPART -> 2
        STRUCTURE_ROAD -> 1
        else -> 1
    }

    private fun constructionPriorityWeight(site: ConstructionSite): Double = when (constructionPriority(site)) {
        0 -> 2.5
        1 -> 1.75
        2 -> 1.25
        3 -> 1.0
        4 -> 0.5
        else -> 1.0
    }

    private fun constructionPriority(site: ConstructionSite): Int = when (site.structureType) {
        STRUCTURE_SPAWN -> 0
        STRUCTURE_EXTENSION,
        STRUCTURE_TOWER -> 1

        STRUCTURE_STORAGE,
        STRUCTURE_TERMINAL,
        STRUCTURE_CONTAINER -> 2

        STRUCTURE_RAMPART -> 3
        STRUCTURE_ROAD -> 4
        else -> 5
    }
}
