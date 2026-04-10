package room.planning

import heuristics.ConstructionSiteHeuristics
import heuristics.EnergyHeuristics
import heuristics.RoomHeuristics
import memory.stage
import room.enums.RoomStage
import screeps.api.BUILD_POWER
import screeps.api.ConstructionSite
import screeps.api.Room
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

object ConstructionPlanningPolicy {
    fun analyze(
        room: Room,
        planningCache: RoomPlanningCache?,
        constructionSites: Array<ConstructionSite>,
        assignedSiteCounts: Map<String, Int>,
        mode: RoomWorkMode,
        plannedWorkerWorkUnits: Int
    ): ConstructionPlanningAnalysis {
        val activeSites = selectActiveSites(room, constructionSites, assignedSiteCounts, mode)
        val targetSite = selectTargetSite(room, activeSites, assignedSiteCounts)

        if (activeSites.isEmpty() || targetSite == null) {
            return ConstructionPlanningAnalysis(
                activeSites = emptyList(),
                targetSite = null,
                demand = TaskDemand(
                    priority = 0,
                    minimumWorkUnits = 0,
                    desiredWorkUnits = 0,
                    maxWorkUnits = 0
                )
            )
        }

        val maxWorkUnits = maxWorkUnits(activeSites, plannedWorkerWorkUnits)
        val demand = TaskDemand(
            priority = priority(targetSite),
            minimumWorkUnits = minimumWorkUnits(targetSite, maxWorkUnits, plannedWorkerWorkUnits, mode),
            desiredWorkUnits = desiredWorkUnits(room, planningCache, activeSites, targetSite)
                .coerceAtMost(maxWorkUnits),
            maxWorkUnits = maxWorkUnits
        )

        return ConstructionPlanningAnalysis(
            activeSites = activeSites,
            targetSite = targetSite,
            demand = demand
        )
    }

    fun nextTargetSite(
        room: Room,
        activeSites: List<ConstructionSite>,
        assignedSiteCounts: Map<String, Int>
    ): ConstructionSite? = selectTargetSite(room, activeSites, assignedSiteCounts)

    private fun desiredWorkUnits(
        room: Room,
        planningCache: RoomPlanningCache?,
        activeSites: List<ConstructionSite>,
        primarySite: ConstructionSite
    ): Int {
        val remainingWork = planningCache?.weightedRemainingConstructionWork
            ?: ConstructionSiteHeuristics.weightedRemainingWork(activeSites.toTypedArray())
        if (remainingWork <= 0) {
            return 0
        }

        val targetTicks = constructionTargetTicks(room, planningCache, primarySite)
        val requiredProgressPerTick = ceil(remainingWork.toDouble() / targetTicks).toInt()
        return ceil(requiredProgressPerTick.toDouble() / BUILD_POWER.toDouble()).toInt().coerceAtLeast(1)
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
        val modeModifier = when {
            primarySitePriority <= 1 -> 0.75
            else -> 1.0
        }

        return max(100, (baseTicks * energyModifier * priorityModifier * modeModifier).roundToInt())
    }

    private fun maxWorkUnits(activeSites: List<ConstructionSite>, plannedWorkerWorkUnits: Int): Int =
        activeSites.sumOf(ConstructionSiteHeuristics::siteBuildersCapacity) * plannedWorkerWorkUnits.coerceAtLeast(1)

    private fun selectActiveSites(
        room: Room,
        constructionSites: Array<ConstructionSite>,
        assignedSiteCounts: Map<String, Int>,
        mode: RoomWorkMode
    ): List<ConstructionSite> {
        if (constructionSites.isEmpty()) {
            return emptyList()
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
            .take(activeSiteWindowSize(mode))
    }

    private fun selectTargetSite(
        room: Room,
        activeSites: List<ConstructionSite>,
        assignedSiteCounts: Map<String, Int>
    ): ConstructionSite? {
        if (activeSites.isEmpty()) {
            return null
        }

        return activeSites.sortedWith(
            compareBy<ConstructionSite> { assignedSiteCounts[it.id] ?: 0 }
                .thenBy(ConstructionSiteHeuristics::sitePriority)
                .thenBy { room.controller?.pos?.getRangeTo(it.pos) ?: 0 }
                .thenByDescending { it.progressTotal - it.progress }
        ).firstOrNull()
    }

    private fun stage(room: Room, planningCache: RoomPlanningCache?): RoomStage =
        planningCache?.stage ?: room.memory.stage

    private fun activeSiteWindowSize(mode: RoomWorkMode): Int = when (mode) {
        RoomWorkMode.Bootstrap -> 1
        RoomWorkMode.Recovery -> 2
        RoomWorkMode.Steady -> 3
        RoomWorkMode.Surplus -> 4
    }

    private fun priority(targetSite: ConstructionSite): Int = when (ConstructionSiteHeuristics.sitePriority(targetSite)) {
        0 -> 300
        1 -> 260
        2 -> 180
        3 -> 120
        4 -> 90
        else -> 60
    }

    private fun minimumWorkUnits(
        targetSite: ConstructionSite,
        maxWorkUnits: Int,
        plannedWorkerWorkUnits: Int,
        mode: RoomWorkMode
    ): Int = when {
        ConstructionSiteHeuristics.sitePriority(targetSite) <= 1 ->
            minOf(maxWorkUnits, plannedWorkerWorkUnits.coerceAtLeast(1))

        ConstructionSiteHeuristics.sitePriority(targetSite) == 2 && mode == RoomWorkMode.Bootstrap ->
            minOf(maxWorkUnits, plannedWorkerWorkUnits.coerceAtLeast(1))

        else -> 0
    }
}
