package actors

import creep.BodyRecipe
import screeps.api.ConstructionSite
import screeps.api.FIND_SOURCES
import screeps.api.Room
import kotlin.math.roundToInt

object RoomWorkAllocator {
    fun plan(
        room: Room,
        planningCache: RoomPlanningCache?,
        roomWorkers: List<CreepStatus>,
        constructionSites: Array<ConstructionSite>,
        assignedConstructionSiteCounts: Map<String, Int>,
        assignedTransferTargetCounts: Map<String, Int>
    ): RoomWorkforcePlan {
        val mode = selectMode(room, planningCache, roomWorkers)
        val spawnProfile = selectSpawnProfile(mode)
        val plannedWorkerWorkUnits = plannedWorkerWorkUnits(room, spawnProfile)
        val constructionAnalysis = ConstructionPlanningPolicy.analyze(
            room = room,
            planningCache = planningCache,
            constructionSites = constructionSites,
            assignedSiteCounts = assignedConstructionSiteCounts,
            mode = mode,
            plannedWorkerWorkUnits = plannedWorkerWorkUnits
        )
        val transferAnalysis = EnergyTransferPlanningPolicy.analyze(
            room = room,
            assignedTargetCounts = assignedTransferTargetCounts,
            mode = mode,
            plannedWorkerWorkUnits = plannedWorkerWorkUnits
        )
        val totalTargetWorkUnits = totalTargetWorkUnits(room, planningCache, mode, plannedWorkerWorkUnits)
        val workUnitsByTask = allocateWorkUnits(
            totalTargetWorkUnits = totalTargetWorkUnits,
            constructionDemand = constructionAnalysis.demand,
            transferDemand = transferAnalysis.demand
        )

        return RoomWorkforcePlan(
            mode = mode,
            spawnProfile = spawnProfile,
            totalTargetWorkUnits = totalTargetWorkUnits,
            plannedWorkerWorkUnits = plannedWorkerWorkUnits,
            workUnitsByTask = workUnitsByTask,
            construction = ConstructionWorkPlan(
                demand = constructionAnalysis.demand,
                activeSiteIds = constructionAnalysis.activeSites.map { site -> site.id },
                targetSiteId = constructionAnalysis.targetSite?.id
            ),
            energyTransfer = EnergyTransferWorkPlan(
                demand = transferAnalysis.demand,
                activeTargetIds = transferAnalysis.activeTargets.map { target -> target.id },
                targetId = transferAnalysis.target?.id
            )
        )
    }

    private fun selectMode(
        room: Room,
        planningCache: RoomPlanningCache?,
        roomWorkers: List<CreepStatus>
    ): RoomWorkMode {
        val workerCount = roomWorkers.size
        val cache = planningCache ?: return RoomWorkMode.Bootstrap

        return when {
            cache.controllerLevel <= 2 || cache.extensionCount < 5 -> RoomWorkMode.Bootstrap
            cache.controllerTicksToDowngrade <= 2_500 -> RoomWorkMode.Recovery
            workerCount == 0 -> RoomWorkMode.Recovery
            cache.hasStorage && cache.bufferedEnergy >= 5_000 -> RoomWorkMode.Surplus
            cache.bufferedEnergy < 300 && room.energyAvailable < 200 -> RoomWorkMode.Recovery
            else -> RoomWorkMode.Steady
        }
    }

    private fun selectSpawnProfile(mode: RoomWorkMode): WorkerSpawnProfile = when (mode) {
        RoomWorkMode.Bootstrap,
        RoomWorkMode.Recovery -> WorkerSpawnProfile.Bootstrap

        RoomWorkMode.Steady -> WorkerSpawnProfile.Standard
        RoomWorkMode.Surplus -> WorkerSpawnProfile.Heavy
    }

    private fun plannedWorkerWorkUnits(room: Room, spawnProfile: WorkerSpawnProfile): Int {
        val controllerId = room.controller?.id ?: room.name
        val sampleAssignment = CreepAssignment.ControllerProgress(
            roomName = room.name,
            controllerId = controllerId
        )

        return BodyRecipe.plannedWorkUnits(
            energyBudget = room.energyCapacityAvailable,
            assignment = sampleAssignment,
            profile = spawnProfile
        ).coerceAtLeast(1)
    }

    private fun totalTargetWorkUnits(
        room: Room,
        planningCache: RoomPlanningCache?,
        mode: RoomWorkMode,
        plannedWorkerWorkUnits: Int
    ): Int {
        val sources = planningCache?.sourceCount ?: room.find(FIND_SOURCES).size
        if (sources <= 0) {
            return 0
        }

        val sustainableIncome = planningCache?.sustainableIncome ?: sources * 10
        val sourceOpenSides = planningCache?.totalSourceOpenSides ?: sources
        val maxWorkUnitsByConcurrency = sourceOpenSides * plannedWorkerWorkUnits
        val utilization = when (mode) {
            RoomWorkMode.Bootstrap -> 0.7
            RoomWorkMode.Recovery -> 0.6
            RoomWorkMode.Steady -> 0.9
            RoomWorkMode.Surplus -> 1.0
        }

        val desiredWorkUnits = (sustainableIncome.toDouble() * utilization).roundToInt().coerceAtLeast(1)
        return desiredWorkUnits.coerceAtMost(maxWorkUnitsByConcurrency)
    }

    private fun allocateWorkUnits(
        totalTargetWorkUnits: Int,
        constructionDemand: TaskDemand,
        transferDemand: TaskDemand
    ): Map<RoomTaskKind, Int> {
        val allocations = mutableMapOf(
            RoomTaskKind.EnergyTransfer to 0,
            RoomTaskKind.Construction to 0,
            RoomTaskKind.ControllerProgress to 0
        )

        var remaining = totalTargetWorkUnits.coerceAtLeast(0)
        val prioritized = listOf(
            RoomTaskKind.EnergyTransfer to transferDemand,
            RoomTaskKind.Construction to constructionDemand
        ).sortedByDescending { (_, demand) -> demand.priority }

        prioritized.forEach { (task, demand) ->
            val grant = minOf(remaining, demand.normalizedMinimumWorkUnits)
            allocations[task] = grant
            remaining -= grant
        }

        prioritized.forEach { (task, demand) ->
            if (remaining <= 0) {
                return@forEach
            }

            val current = allocations[task] ?: 0
            val additionalDesired = (demand.normalizedDesiredWorkUnits - current).coerceAtLeast(0)
            val grant = minOf(remaining, additionalDesired)
            allocations[task] = current + grant
            remaining -= grant
        }

        allocations[RoomTaskKind.ControllerProgress] = remaining.coerceAtLeast(0)
        return allocations
    }
}
