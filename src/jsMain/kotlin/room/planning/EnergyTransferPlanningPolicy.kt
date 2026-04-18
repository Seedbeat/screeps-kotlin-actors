package room.planning

import room.find
import screeps.api.Room
import screeps.api.STRUCTURE_EXTENSION
import screeps.api.STRUCTURE_SPAWN
import screeps.api.StoreOwner
import screeps.api.structures.Structure
import store.energyStore
import kotlin.math.ceil

object EnergyTransferPlanningPolicy {
    fun analyze(
        room: Room,
        assignedTargetCounts: Map<String, Int>,
        mode: RoomWorkMode,
        plannedWorkerWorkUnits: Int
    ): EnergyTransferPlanningAnalysis {
        val activeTargets = selectActiveTargets(room, assignedTargetCounts, mode)
        val target = selectTarget(activeTargets, assignedTargetCounts)

        if (activeTargets.isEmpty() || target == null) {
            return EnergyTransferPlanningAnalysis(
                activeTargets = emptyList(),
                target = null,
                demand = TaskDemand(
                    priority = 0,
                    minimumWorkUnits = 0,
                    desiredWorkUnits = 0,
                    maxWorkUnits = 0
                )
            )
        }

        val maxWorkUnits = activeTargets.size * plannedWorkerWorkUnits.coerceAtLeast(1)
        val demand = TaskDemand(
            priority = priority(target),
            minimumWorkUnits = minimumWorkUnits(activeTargets, maxWorkUnits, plannedWorkerWorkUnits),
            desiredWorkUnits = desiredWorkUnits(activeTargets, mode).coerceAtMost(maxWorkUnits),
            maxWorkUnits = maxWorkUnits
        )

        return EnergyTransferPlanningAnalysis(
            activeTargets = activeTargets,
            target = target,
            demand = demand
        )
    }

    fun nextTarget(
        activeTargets: List<Structure>,
        assignedTargetCounts: Map<String, Int>
    ): Structure? = selectTarget(activeTargets, assignedTargetCounts)

    private fun selectActiveTargets(
        room: Room,
        assignedTargetCounts: Map<String, Int>,
        mode: RoomWorkMode
    ): List<Structure> = room.find.my.structures.run {
        (extensions.asList() + spawns.asList() + towers.asList())
            .filter { target ->
                target.storeOwner.energyStore.isNotFull && (assignedTargetCounts[target.id] ?: 0) < 1
            }
            .sortedWith(
                compareByDescending(::priority)
                    .thenBy { assignedTargetCounts[it.id] ?: 0 }
                    .thenByDescending { it.storeOwner.energyStore.free }
            )
            .take(activeTargetWindowSize(mode))
    }

    private fun selectTarget(
        activeTargets: List<Structure>,
        assignedTargetCounts: Map<String, Int>
    ): Structure? = activeTargets.sortedWith(
        compareBy<Structure> { assignedTargetCounts[it.id] ?: 0 }
            .thenByDescending(::priority)
            .thenByDescending { it.storeOwner.energyStore.free }
    ).firstOrNull()

    private fun desiredWorkUnits(activeTargets: List<Structure>, mode: RoomWorkMode): Int {
        val weightedDeficit = activeTargets.sumOf { target ->
            val weight = when (target.structureType) {
                STRUCTURE_EXTENSION,
                STRUCTURE_SPAWN -> 100

                else -> 60
            }

            ceil(target.storeOwner.energyStore.free.toDouble() * weight.toDouble() / 100.0).toInt()
        }

        if (weightedDeficit <= 0) {
            return 0
        }

        val targetTicks = when (mode) {
            RoomWorkMode.Bootstrap -> 15
            RoomWorkMode.Recovery -> 20
            RoomWorkMode.Steady -> 30
            RoomWorkMode.Surplus -> 40
        }

        return ceil(weightedDeficit.toDouble() / targetTicks.toDouble()).toInt().coerceAtLeast(1)
    }

    private fun activeTargetWindowSize(mode: RoomWorkMode): Int = when (mode) {
        RoomWorkMode.Bootstrap -> 2
        RoomWorkMode.Recovery -> 3
        RoomWorkMode.Steady -> 4
        RoomWorkMode.Surplus -> 6
    }

    private fun priority(target: Structure): Int = when (target.structureType) {
        STRUCTURE_EXTENSION,
        STRUCTURE_SPAWN -> 320

        else -> 220
    }

    private fun minimumWorkUnits(
        activeTargets: List<Structure>,
        maxWorkUnits: Int,
        plannedWorkerWorkUnits: Int
    ): Int {
        val hasCriticalTarget = activeTargets.any { target ->
            target.structureType == STRUCTURE_EXTENSION || target.structureType == STRUCTURE_SPAWN
        }

        return if (hasCriticalTarget) {
            minOf(maxWorkUnits, plannedWorkerWorkUnits.coerceAtLeast(1))
        } else {
            0
        }
    }

    private val Structure.storeOwner: StoreOwner
        get() = unsafeCast<StoreOwner>()
}
