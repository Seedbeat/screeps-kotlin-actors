package actors

import actors.CreepCommand.*
import actors.CreepRequest.StatusRequest
import actors.CreepResponse.StatusResponse
import actors.CreepResponse.UnassignResponse
import actors.RoomRequest.ReleaseResource
import actors.RoomRequest.TryAcquireResource
import actors.assignments.ControllerUpkeepPhase
import actors.base.GameCreepBinding
import actors.base.IActorBinding
import actors.base.Lifecycle
import creep.enums.State
import memory.*
import screeps.api.*
import screeps.api.structures.StructureController
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class CreepActor(
    id: String
) : ActorBase<Creep, CreepCommand, CreepRequest, CreepResponse<*>>(id),
    IActorBinding<Creep> by GameCreepBinding(id),
    ILogging by Logging<CreepActor>(id, LogLevel.INFO) {

    override suspend fun processLifecycle(msg: Lifecycle) = when (msg) {
        is Lifecycle.Bootstrap -> Unit
        is Lifecycle.Tick -> executeAssignment()
    }

    override suspend fun processCommand(msg: CreepCommand) = when (msg) {
        Noop -> Unit
        is Assign -> assign(msg.assignment)
        is SetLockedResourceId -> setLockedResourceId(msg.resourceId)
        ClearAssignment -> {
            clearAssignmentState()
            Unit
        }
    }

    override suspend fun processRequest(msg: CreepRequest): CreepResponse<*> = when (msg) {
        StatusRequest -> StatusResponse(result = status())
        CreepRequest.Unassign -> UnassignResponse(result = clearAssignmentState())
    }

    override fun onDestroy() {
        clearDestroyedAssignmentState()
    }

    private suspend fun executeAssignment() {
        when (val assignment = self.memory.assignmentOrNull()) {
            null -> {
                self.memory.state = State.UNASSIGNED
                releaseLockedResourceIfHeld()
            }

            is CreepAssignment.ControllerUpkeep -> executeControllerUpkeep(assignment)
        }
    }

    private suspend fun assign(assignment: CreepAssignment) {
        releaseLockedResourceIfHeld()
        self.memory.setAssignment(assignment)
        self.memory.state = State.UNASSIGNED
    }

    private suspend fun executeControllerUpkeep(assignment: CreepAssignment.ControllerUpkeep) {
        val creepMemory = self.memory
        val source = Game.getObjectById<Source>(assignment.sourceId)
        val controller = Game.getObjectById<StructureController>(assignment.controllerId)

        if (source == null || controller == null) {
            clearAssignmentState()
            return
        }

        when (creepMemory.controllerUpkeepPhase()) {
            ControllerUpkeepPhase.HARVEST -> executeHarvestPhase(assignment, source)
            ControllerUpkeepPhase.UPGRADE -> executeUpgradePhase(assignment, controller)
        }
    }

    private suspend fun executeHarvestPhase(
        assignment: CreepAssignment.ControllerUpkeep,
        source: Source
    ) {
        val usedCapacity = self.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0
        val freeCapacity = self.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0

        if (freeCapacity <= 0) {
            switchToUpgradePhase()
            return
        }

        if (!ensureSourceLock(assignment)) {
            if (usedCapacity > 0) {
                switchToUpgradePhase()
            }
            return
        }

        self.memory.state = State.SOURCE_WORK
        self.memory.workObjectId = source.id

        if (source.energy <= 0 && usedCapacity > 0) {
            switchToUpgradePhase()
            return
        }

        when (val code = self.harvest(source)) {
            OK -> {
                val remainingFreeCapacity = self.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0
                val remainingEnergy = source.energy

                if (remainingFreeCapacity <= 0 || (remainingEnergy <= 0 && usedCapacityNow() > 0)) {
                    switchToUpgradePhase()
                }
            }

            ERR_NOT_IN_RANGE -> {
                self.moveTo(source)
            }

            ERR_NOT_ENOUGH_RESOURCES -> {
                if (usedCapacityNow() > 0) {
                    switchToUpgradePhase()
                }
            }

            ERR_INVALID_TARGET -> {
                clearAssignmentState()
            }

            else -> {
                log.error("Controller upkeep harvest failed for $id with code $code")
            }
        }
    }

    private suspend fun executeUpgradePhase(
        assignment: CreepAssignment.ControllerUpkeep,
        controller: StructureController
    ) {
        releaseLockedResourceIfHeld()

        if (usedCapacityNow() <= 0) {
            switchToHarvestPhase(assignment)
            return
        }

        self.memory.state = State.TARGET_WORK
        self.memory.workObjectId = controller.id

        when (val code = self.upgradeController(controller)) {
            OK -> {
                if (usedCapacityNow() <= 0) {
                    switchToHarvestPhase(assignment)
                }
            }

            ERR_NOT_IN_RANGE -> {
                self.moveTo(controller)
            }

            ERR_NOT_ENOUGH_RESOURCES -> {
                switchToHarvestPhase(assignment)
            }

            ERR_INVALID_TARGET -> {
                clearAssignmentState()
            }

            else -> {
                log.error("Controller upkeep upgrade failed for $id with code $code")
            }
        }
    }

    private suspend fun ensureSourceLock(assignment: CreepAssignment.ControllerUpkeep): Boolean {
        val lockedResourceId = self.memory.lockedObjectId

        if (lockedResourceId == assignment.sourceId) {
            return true
        }

        if (lockedResourceId.isNotEmpty()) {
            releaseLockedResourceIfHeld()
        }

        val acquired: Boolean? = requestFrom(
            assignment.roomName,
            TryAcquireResource(ownerId = id, resourceId = assignment.sourceId)
        )

        if (acquired == true) {
            setLockedResourceId(assignment.sourceId)
            return true
        }

        return false
    }

    private suspend fun switchToHarvestPhase(assignment: CreepAssignment.ControllerUpkeep) {
        releaseLockedResourceIfHeld()
        self.memory.controllerUpkeepPhase(ControllerUpkeepPhase.HARVEST)
        self.memory.state = State.UNASSIGNED
        self.memory.assignmentRoom = assignment.roomName
    }

    private suspend fun switchToUpgradePhase() {
        releaseLockedResourceIfHeld()
        self.memory.controllerUpkeepPhase(ControllerUpkeepPhase.UPGRADE)
        self.memory.state = State.UNASSIGNED
    }

    private suspend fun clearAssignmentState(): Boolean {
        releaseLockedResourceIfHeld()

        val creepMemory = self.memory
        creepMemory.state = State.UNASSIGNED
        creepMemory.clearAssignment()
        setLockedResourceId(null)
        return true
    }

    private fun setLockedResourceId(resourceId: String?) {
        self.memory.lockedObjectId = resourceId ?: ""
    }

    private suspend fun releaseLockedResourceIfHeld() {
        val resourceId = self.memory.lockedObjectId.takeIf { it.isNotEmpty() } ?: return
        val roomName = self.memory.assignmentOrNull()?.roomName
            ?: self.memory.assignmentRoom.takeIf { it.isNotBlank() }
            ?: selfOrNull?.room?.name
            ?: return

        val released: Boolean? = requestFrom(
            roomName,
            ReleaseResource(ownerId = id, resourceId = resourceId)
        )

        if (released == true) {
            setLockedResourceId(null)
        }
    }

    private fun clearDestroyedAssignmentState() {
        val creepMemory = selfOrNull?.memory ?: Memory.creeps[id] ?: return
        creepMemory.state = State.UNASSIGNED
        creepMemory.clearAssignment()
        creepMemory.lockedObjectId = ""
    }

    private fun status(): CreepStatus = CreepStatus(
        actorId = id,
        homeRoom = self.memory.homeRoom,
        assignmentRoom = self.memory.assignmentRoom,
        currentRoom = self.room.name,
        assignment = self.memory.assignmentOrNull(),
        capabilities = CreepCapabilities.from(self),
        lockedResourceId = self.memory.lockedObjectId.takeIf { it.isNotBlank() }
    )

    private fun usedCapacityNow(): Int = self.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0
}
