package actors

import actors.RoomRequest.ReleaseResource
import actors.RoomRequest.TryAcquireAnyResource
import actors.assignments.ControllerUpkeepPhase
import actors.base.ActorApi
import actors.base.ActorBinding
import memory.assignment
import memory.homeRoom
import memory.lockedObjectId
import screeps.api.*
import screeps.api.structures.StructureController
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class CreepIntentService<T>(
    api: T
) : ActorApi by api,
    ActorBinding<Creep> by api,
    ILogging by Logging<CreepIntentService<T>>(api.id, LogLevel.INFO)
        where T : ActorApi,
              T : ActorBinding<Creep> {

    suspend fun executeAssignment() {
        when (val assignment = self.memory.assignment.value) {
            null -> releaseLockedResourceIfHeld()
            is CreepAssignment.ControllerUpkeep -> executeControllerUpkeep(assignment)
        }
    }

    suspend fun assign(assignment: CreepAssignment) {
        releaseLockedResourceIfHeld()
        self.memory.assignment.value = assignment
    }

    suspend fun clearAssignmentState(): Boolean {
        releaseLockedResourceIfHeld()
        self.memory.assignment.value = null
        setLockedResourceId(null)
        return true
    }

    fun setLockedResourceId(resourceId: String?) {
        self.memory.lockedObjectId = resourceId ?: ""
    }

    fun status(): CreepStatus = CreepStatus(
        actorId = id,
        homeRoom = self.memory.homeRoom,
        currentRoom = self.room.name,
        assignment = self.memory.assignment.value,
        capabilities = CreepCapabilities.from(self),
        lockedResourceId = self.memory.lockedObjectId.takeIf { it.isNotBlank() }
    )

    private suspend fun executeControllerUpkeep(assignment: CreepAssignment.ControllerUpkeep) {
        val controller = Game.getObjectById<StructureController>(assignment.controllerId)

        if (controller == null) {
            clearAssignmentState()
            return
        }

        when (self.memory.assignment.phase) {
            ControllerUpkeepPhase.HARVEST -> executeHarvestPhase(assignment)
            ControllerUpkeepPhase.UPGRADE -> executeUpgradePhase(assignment, controller)
        }
    }

    private suspend fun executeHarvestPhase(assignment: CreepAssignment.ControllerUpkeep) {
        val usedCapacity = self.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0
        val freeCapacity = self.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0

        if (freeCapacity <= 0) {
            switchToUpgradePhase()
            return
        }

        val source = resolveNearestAvailableSource(assignment)
        if (source == null) {
            if (usedCapacity > 0) {
                switchToUpgradePhase()
            }
            return
        }

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

            ERR_NOT_IN_RANGE -> self.moveTo(source)

            ERR_NOT_ENOUGH_RESOURCES -> {
                if (usedCapacityNow() > 0) {
                    switchToUpgradePhase()
                }
            }

            ERR_INVALID_TARGET -> clearAssignmentState()

            else -> log.error("Controller upkeep harvest failed with code $code")
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

        when (val code = self.upgradeController(controller)) {
            OK -> {
                if (usedCapacityNow() <= 0) {
                    switchToHarvestPhase(assignment)
                }
            }

            ERR_NOT_IN_RANGE -> self.moveTo(controller)
            ERR_NOT_ENOUGH_RESOURCES -> switchToHarvestPhase(assignment)
            ERR_INVALID_TARGET -> clearAssignmentState()
            else -> log.error("Controller upkeep upgrade failed with code $code")
        }
    }

    private suspend fun resolveNearestAvailableSource(assignment: CreepAssignment.ControllerUpkeep): Source? {
        val lockedResourceId = self.memory.lockedObjectId.takeIf { it.isNotEmpty() }
        val lockedSource = lockedResourceId?.let { Game.getObjectById<Source>(it) }

        if (lockedSource != null && lockedSource.energy > 0) {
            return lockedSource
        }

        if (lockedResourceId != null) {
            releaseLockedResourceIfHeld()
        }

        val acquiredResourceId = ensurePreferredResourceLock(assignment.roomName) ?: return null
        return Game.getObjectById(acquiredResourceId)
    }

    private suspend fun ensurePreferredResourceLock(roomName: String): String? {
        val lockedResourceId = self.memory.lockedObjectId
        if (lockedResourceId.isNotEmpty()) {
            return lockedResourceId
        }

        val acquiredResourceId: String? = requestFrom(
            roomName,
            TryAcquireAnyResource(
                ownerId = id,
                near = self.pos,
                type = RoomResourceType.SOURCE
            )
        )

        if (acquiredResourceId != null) {
            setLockedResourceId(acquiredResourceId)
        }

        return acquiredResourceId
    }

    private suspend fun switchToHarvestPhase(assignment: CreepAssignment.ControllerUpkeep) {
        releaseLockedResourceIfHeld()
        self.memory.assignment.phase = ControllerUpkeepPhase.HARVEST
    }

    private suspend fun switchToUpgradePhase() {
        releaseLockedResourceIfHeld()
        self.memory.assignment.phase = ControllerUpkeepPhase.UPGRADE
    }

    private suspend fun releaseLockedResourceIfHeld() {
        val resourceId = self.memory.lockedObjectId.takeIf { it.isNotEmpty() } ?: return
        val roomName = self.memory.assignment.value?.roomName
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

    private fun usedCapacityNow(): Int = self.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0
}
