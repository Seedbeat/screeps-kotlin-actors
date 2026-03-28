package actors

import actors.heuristics.ConstructionSiteHeuristics
import actors.heuristics.EnergyHeuristics
import actors.heuristics.RoomHeuristics
import creep.BodyRecipe
import memory.stage
import room.RoomStage
import screeps.api.BUILD_POWER
import screeps.api.ConstructionSite
import screeps.api.Room
import screeps.api.WORK
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

        val stage = stage(room, planningCache)
        val maxBuildersCap = RoomHeuristics.maxBuildersCap(stage)

        val targetSite = selectTargetSite(room, constructionSites, assignedSiteCounts)
            ?: return ConstructionPlanningAnalysis(
                targetSite = null,
                desiredThroughput = 0,
                builderCountCap = maxBuildersCap,
                targetSiteCapacity = 0,
                targetSiteAssigned = 0
            )

        val targetSiteCapacity = ConstructionSiteHeuristics.siteBuildersCapacity(targetSite)
        val targetSiteAssigned = assignedSiteCounts[targetSite.id] ?: 0

        return ConstructionPlanningAnalysis(
            targetSite = targetSite,
            desiredThroughput = desiredThroughput(room, planningCache, targetSite),
            builderCountCap = maxBuildersCap,
            targetSiteCapacity = targetSiteCapacity,
            targetSiteAssigned = targetSiteAssigned
        )
    }

    fun spawnableThroughput(room: Room, targetSite: ConstructionSite): Int =
        constructionThroughput(room, targetSite, room.energyAvailable)

    private fun desiredThroughput(
        room: Room,
        planningCache: RoomPlanningCache?,
        primarySite: ConstructionSite
    ): Int {
        val remainingWork = planningCache?.weightedRemainingConstructionWork ?: 0
        if (remainingWork <= 0) {
            return 0
        }

        val targetTicks = constructionTargetTicks(room, planningCache, primarySite)
        val requiredThroughput = ceil(remainingWork.toDouble() / targetTicks).toInt().coerceAtLeast(BUILD_POWER)
        val maxThroughput = maxThroughputCap(room, planningCache, primarySite)
        return requiredThroughput.coerceAtMost(maxThroughput)
    }

    private fun constructionTargetTicks(
        room: Room,
        planningCache: RoomPlanningCache?,
        primarySite: ConstructionSite
    ): Int {

        val stage = stage(room, planningCache)
        val baseTicks = RoomHeuristics.targetBuildTicks(stage)

        val energyRatio = EnergyHeuristics.energyRatio(room)
        val energyModifier = EnergyHeuristics.energyRatioModifier(energyRatio)

        val primarySitePriority = ConstructionSiteHeuristics.sitePriority(primarySite)
        val priorityModifier = ConstructionSiteHeuristics.priorityModifier(primarySitePriority)

        return max(100, (baseTicks * energyModifier * priorityModifier).roundToInt())
    }

    private fun maxThroughputCap(
        room: Room,
        planningCache: RoomPlanningCache?,
        primarySite: ConstructionSite
    ): Int {
        val stage = stage(room, planningCache)
        val maxBuilders = RoomHeuristics.maxBuildersCap(stage)

        val energyBudget = capacityAvailable(room, planningCache)
        val perBuilderThroughput = max(BUILD_POWER, constructionThroughput(room, primarySite, energyBudget))

        return maxBuilders * perBuilderThroughput
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
            .filter { site ->
                (assignedSiteCounts[site.id] ?: 0) < ConstructionSiteHeuristics.siteBuildersCapacity(site)
            }
            .sortedWith(
                compareBy(ConstructionSiteHeuristics::sitePriority)
                    .thenBy { assignedSiteCounts[it.id] ?: 0 }
                    .thenBy { room.controller?.pos?.getRangeTo(it.pos) ?: 0 }
                    .thenByDescending { it.progressTotal - it.progress }
            )
            .firstOrNull()
    }

    private fun stage(room: Room, planningCache: RoomPlanningCache?): RoomStage =
        planningCache?.stage ?: room.memory.stage

    private fun capacityAvailable(room: Room, planningCache: RoomPlanningCache?): Int =
        planningCache?.energyCapacityAvailable ?: room.energyCapacityAvailable
}
