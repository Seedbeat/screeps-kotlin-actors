package actors

import actors.base.ActorApi
import actors.base.ActorBinding
import actors.base.IntentResultType
import memory.homeRoom
import memory.planningCache
import room.constructionSites
import room.structures
import screeps.api.Room
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class RoomPlanningService<T>(
    api: T
) : ActorApi by api,
    ActorBinding<Room> by api,
    ILogging by Logging<RoomPlanningService<T>>(api.self.name, LogLevel.INFO)
        where T : ActorApi,
              T : ActorBinding<Room> {
    suspend fun ensureControllerSurvival(): IntentResultType {
        val controller = self.controller
            ?: return IntentResultType.RETAINED

        val roomWorkers = roomWorkers()
        val alreadyAssigned = roomWorkers.any { status ->
            val assignment = status.assignment
            assignment is CreepAssignment.ControllerUpkeep &&
                    assignment.roomName == self.name &&
                    assignment.controllerId == controller.id
        }

        if (alreadyAssigned) {
            return IntentResultType.COMPLETED
        }

        if (assignIfAvailable(
                actorId = availableUnassignedWorkerActorId(roomWorkers),
                assignment = CreepAssignment.ControllerUpkeep(
                    roomName = self.name,
                    controllerId = controller.id
                )
            )
        ) {
            return IntentResultType.COMPLETED
        }

        if (!spawnWorker(
                assignment = CreepAssignment.ControllerUpkeep(
                    roomName = self.name,
                    controllerId = controller.id
                ),
                profile = WorkerSpawnProfile.Bootstrap
            )
        ) {
            return IntentResultType.RETAINED
        }

        return IntentResultType.COMPLETED
    }

    suspend fun planWorkforce(): IntentResultType {
        val controller = self.controller
            ?: return IntentResultType.RETAINED

        val constructionSites = self.constructionSites.my
        val roomWorkers = roomWorkers()
        val assignedSiteCounts = roomWorkers
            .mapNotNull { status -> (status.assignment as? CreepAssignment.Construction)?.constructionSiteId }
            .groupingBy { siteId -> siteId }
            .eachCount()
        val assignedTransferTargetCounts = roomWorkers
            .mapNotNull { status -> (status.assignment as? CreepAssignment.EnergyTransfer)?.targetId }
            .groupingBy { targetId -> targetId }
            .eachCount()
        val plan = RoomWorkAllocator.plan(
            room = self,
            planningCache = self.memory.planningCache,
            roomWorkers = roomWorkers,
            constructionSites = constructionSites,
            assignedConstructionSiteCounts = assignedSiteCounts,
            assignedTransferTargetCounts = assignedTransferTargetCounts
        )
        val currentWorkByTask = currentWorkByTask(roomWorkers)
        val currentConstructionWork = currentWorkByTask[RoomTaskKind.Construction] ?: 0
        val currentTransferWork = currentWorkByTask[RoomTaskKind.EnergyTransfer] ?: 0
        val currentControllerProgressWork = currentWorkByTask[RoomTaskKind.ControllerProgress] ?: 0
        val currentTotalWork = currentAssignedWork(roomWorkers)

        val energyTransferTargetId = plan.energyTransfer.targetId
        if (energyTransferTargetId != null && currentTransferWork < plan.energyTransferWorkUnits) {
            val assignment = CreepAssignment.EnergyTransfer(
                roomName = self.name,
                targetId = energyTransferTargetId,
                goal = CreepAssignment.EnergyTransfer.Goal.UntilFull
            )

            if (tryAssignWorkerForTask(
                    roomWorkers = roomWorkers,
                    assignment = assignment,
                    desiredTask = RoomTaskKind.EnergyTransfer,
                    currentWorkByTask = currentWorkByTask,
                    targetWorkByTask = plan.workUnitsByTask
                )
            ) {
                return IntentResultType.COMPLETED
            }

            if (currentTotalWork < plan.totalTargetWorkUnits && spawnWorker(assignment, plan.spawnProfile)) {
                return IntentResultType.COMPLETED
            }
        }

        val constructionTargetId = plan.construction.targetSiteId
        if (constructionTargetId != null && currentConstructionWork < plan.constructionWorkUnits) {
            val assignment = CreepAssignment.Construction(
                roomName = self.name,
                constructionSiteId = constructionTargetId
            )

            if (tryAssignWorkerForTask(
                    roomWorkers = roomWorkers,
                    assignment = assignment,
                    desiredTask = RoomTaskKind.Construction,
                    currentWorkByTask = currentWorkByTask,
                    targetWorkByTask = plan.workUnitsByTask
                )
            ) {
                return IntentResultType.COMPLETED
            }

            if (currentTotalWork < plan.totalTargetWorkUnits && spawnWorker(assignment, plan.spawnProfile)) {
                return IntentResultType.COMPLETED
            }
        }

        if (currentControllerProgressWork >= plan.controllerProgressWorkUnits) {
            return IntentResultType.COMPLETED
        }

        val controllerProgressAssignment = CreepAssignment.ControllerProgress(
            roomName = self.name,
            controllerId = controller.id
        )

        if (tryAssignWorkerForTask(
                roomWorkers = roomWorkers,
                assignment = controllerProgressAssignment,
                desiredTask = RoomTaskKind.ControllerProgress,
                currentWorkByTask = currentWorkByTask,
                targetWorkByTask = plan.workUnitsByTask
            )
        ) {
            return IntentResultType.COMPLETED
        }

        if (currentTotalWork < plan.totalTargetWorkUnits && spawnWorker(controllerProgressAssignment, plan.spawnProfile)) {
            return IntentResultType.COMPLETED
        }

        return IntentResultType.COMPLETED
    }

    private suspend fun roomWorkers(): List<CreepStatus> = systemRequest(
        payload = SystemRequest.Query.Creeps { creep, assignment ->
            creep.memory.homeRoom == self.name || assignment?.roomName == self.name
        }
    )

    private fun currentAssignedWork(roomWorkers: List<CreepStatus>): Int = roomWorkers.sumOf { status ->
        when (status.assignment) {
            null -> 0
            is CreepAssignment.ControllerUpkeep,
            is CreepAssignment.ControllerProgress,
            is CreepAssignment.Construction,
            is CreepAssignment.EnergyTransfer -> status.capabilities.work
        }
    }

    private fun currentWorkByTask(roomWorkers: List<CreepStatus>): Map<RoomTaskKind, Int> {
        val result = mutableMapOf(
            RoomTaskKind.EnergyTransfer to 0,
            RoomTaskKind.Construction to 0,
            RoomTaskKind.ControllerProgress to 0
        )

        roomWorkers.forEach { status ->
            val task = roomTask(status.assignment) ?: return@forEach
            result[task] = (result[task] ?: 0) + status.capabilities.work
        }

        return result
    }

    private fun roomTask(assignment: CreepAssignment?): RoomTaskKind? = when (assignment) {
        null,
        is CreepAssignment.ControllerUpkeep -> null

        is CreepAssignment.ControllerProgress -> RoomTaskKind.ControllerProgress
        is CreepAssignment.Construction -> RoomTaskKind.Construction
        is CreepAssignment.EnergyTransfer -> RoomTaskKind.EnergyTransfer
    }

    private fun availableUnassignedWorkerActorId(roomWorkers: List<CreepStatus>): String? =
        roomWorkers
            .asSequence()
            .filter { status -> status.assignment == null && status.capabilities.canDoConstruction }
            .sortedByDescending { status -> status.capabilities.work }
            .map { status -> status.actorId }
            .firstOrNull()

    private fun tryAssignWorkerForTask(
        roomWorkers: List<CreepStatus>,
        assignment: CreepAssignment,
        desiredTask: RoomTaskKind,
        currentWorkByTask: Map<RoomTaskKind, Int>,
        targetWorkByTask: Map<RoomTaskKind, Int>
    ): Boolean {
        if (assignIfAvailable(availableUnassignedWorkerActorId(roomWorkers), assignment)) {
            return true
        }

        val reassignmentActorId = availableReassignmentActorId(
            roomWorkers = roomWorkers,
            desiredTask = desiredTask,
            currentWorkByTask = currentWorkByTask,
            targetWorkByTask = targetWorkByTask
        )

        return assignIfAvailable(reassignmentActorId, assignment)
    }

    private fun availableReassignmentActorId(
        roomWorkers: List<CreepStatus>,
        desiredTask: RoomTaskKind,
        currentWorkByTask: Map<RoomTaskKind, Int>,
        targetWorkByTask: Map<RoomTaskKind, Int>
    ): String? {
        val candidateTasks = listOf(
            RoomTaskKind.ControllerProgress,
            RoomTaskKind.Construction,
            RoomTaskKind.EnergyTransfer
        ).filter { task -> task != desiredTask }

        return candidateTasks.firstNotNullOfOrNull { task ->
            val current = currentWorkByTask[task] ?: 0
            val target = targetWorkByTask[task] ?: 0
            if (current <= target) {
                return@firstNotNullOfOrNull null
            }

            roomWorkers
                .asSequence()
                .filter { status -> roomTask(status.assignment) == task }
                .sortedBy { status -> status.capabilities.work }
                .map { status -> status.actorId }
                .firstOrNull()
        }
    }

    private fun assignIfAvailable(actorId: String?, assignment: CreepAssignment): Boolean {
        actorId ?: return false
        CreepCommand.Assign(assignment).sendTo(actorId)
        return true
    }

    private fun spawnWorker(assignment: CreepAssignment, profile: WorkerSpawnProfile): Boolean {
        val spawnActorId = availableSpawnActorId()
            ?: return false

        SpawnCommand.TrySpawnWorker(
            assignment = assignment,
            profile = profile
        ).sendTo(spawnActorId)

        return true
    }

    private fun availableSpawnActorId(): String? =
        self.structures.my.spawns.firstOrNull { it.spawning == null }?.id
}
