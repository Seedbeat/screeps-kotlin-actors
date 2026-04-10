package room.planning

enum class RoomTaskKind {
    EnergyTransfer,
    Construction,
    ControllerProgress
}

enum class RoomWorkMode {
    Bootstrap,
    Recovery,
    Steady,
    Surplus
}

enum class WorkerSpawnProfile {
    Bootstrap,
    Standard,
    Heavy
}

data class TaskDemand(
    val priority: Int,
    val minimumWorkUnits: Int,
    val desiredWorkUnits: Int,
    val maxWorkUnits: Int
) {
    val normalizedMinimumWorkUnits: Int = minimumWorkUnits.coerceAtLeast(0)
    val normalizedDesiredWorkUnits: Int = desiredWorkUnits.coerceAtLeast(normalizedMinimumWorkUnits)
    val normalizedMaxWorkUnits: Int = maxWorkUnits.coerceAtLeast(normalizedDesiredWorkUnits)
}

data class ConstructionWorkPlan(
    val demand: TaskDemand,
    val activeSiteIds: List<String>,
    val targetSiteId: String?
)

data class EnergyTransferWorkPlan(
    val demand: TaskDemand,
    val activeTargetIds: List<String>,
    val targetId: String?
)

data class RoomWorkforcePlan(
    val mode: RoomWorkMode,
    val spawnProfile: WorkerSpawnProfile,
    val totalTargetWorkUnits: Int,
    val plannedWorkerWorkUnits: Int,
    val workUnitsByTask: Map<RoomTaskKind, Int>,
    val construction: ConstructionWorkPlan,
    val energyTransfer: EnergyTransferWorkPlan
) {
    val constructionWorkUnits: Int
        get() = workUnitsByTask[RoomTaskKind.Construction] ?: 0

    val energyTransferWorkUnits: Int
        get() = workUnitsByTask[RoomTaskKind.EnergyTransfer] ?: 0

    val controllerProgressWorkUnits: Int
        get() = workUnitsByTask[RoomTaskKind.ControllerProgress] ?: 0
}
