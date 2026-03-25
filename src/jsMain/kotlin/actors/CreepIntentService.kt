package actors

import actors.CreepCapabilities.Companion.capabilities
import actors.RoomRequest.ReleaseResourceById
import actors.RoomRequest.TryAcquireResourceByType
import actors.assignments.ControllerUpkeepPhase
import actors.base.ActorApi
import actors.base.ActorBinding
import memory.assignment
import memory.homeRoom
import memory.lockedObjectId
import screeps.api.*
import screeps.api.structures.StructureController
import store.energyStore
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
        self.memory.lockedObjectId = resourceId
    }

    fun status(): CreepStatus = CreepStatus(
        actorId = id,
        homeRoom = self.memory.homeRoom,
        currentRoom = self.room.name,
        assignment = self.memory.assignment.value,
        capabilities = self.capabilities,
        lockedResourceId = self.memory.lockedObjectId
    )

    private suspend fun executeControllerUpkeep(assignment: CreepAssignment.ControllerUpkeep) {
        val controller = Game.getObjectById<StructureController>(assignment.controllerId)

        if (controller == null) {
            clearAssignmentState()
            return
        }

        when (self.memory.assignment.phase) {
            ControllerUpkeepPhase.HARVEST -> executeHarvestPhase(assignment)
            ControllerUpkeepPhase.UPGRADE -> executeUpgradePhase(controller)
        }
    }

    private suspend fun executeHarvestPhase(assignment: CreepAssignment.ControllerUpkeep) {
        if (self.energyStore.isFull) {
            switchToUpgradePhase()
            return
        }

        val source = resolveHarvestSource(assignment)
        if (source == null) {
            if (self.energyStore.isNotEmpty) {
                switchToUpgradePhase()
            }
            return
        }

        if (source.energy <= 0 && self.energyStore.isNotEmpty) {
            switchToUpgradePhase()
            return
        }

        when (val code = self.harvest(source)) {
            OK -> {
                if (self.energyStore.isFull || (source.energy <= 0 && self.energyStore.isNotEmpty)) {
                    switchToUpgradePhase()
                }
            }

            ERR_NOT_IN_RANGE -> self.moveTo(source)

            ERR_NOT_ENOUGH_RESOURCES -> {
                if (self.energyStore.isNotEmpty) {
                    switchToUpgradePhase()
                }
            }

            ERR_INVALID_TARGET -> clearAssignmentState()

            else -> log.error("Controller upkeep harvest failed with code $code")
        }
    }

    private suspend fun executeUpgradePhase(controller: StructureController) {
        releaseLockedResourceIfHeld()

        if (self.energyStore.isEmpty) {
            switchToHarvestPhase()
            return
        }

        when (val code = self.upgradeController(controller)) {
            OK -> {
                if (self.energyStore.isEmpty) {
                    switchToHarvestPhase()
                }
            }

            ERR_NOT_IN_RANGE -> self.moveTo(controller)
            ERR_NOT_ENOUGH_RESOURCES -> switchToHarvestPhase()
            ERR_INVALID_TARGET -> clearAssignmentState()
            else -> log.error("Controller upkeep upgrade failed with code $code")
        }
    }

    private suspend fun resolveHarvestSource(assignment: CreepAssignment.ControllerUpkeep): Source? {
        val lockedResourceId = self.memory.lockedObjectId
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
        if (lockedResourceId != null) {
            return lockedResourceId
        }

        val acquiredResourceId: String? = requestFrom(
            actorId = roomName,
            payload = TryAcquireResourceByType(
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

    private suspend fun switchToHarvestPhase() {
        releaseLockedResourceIfHeld()
        self.memory.assignment.phase = ControllerUpkeepPhase.HARVEST
    }

    private suspend fun switchToUpgradePhase() {
        releaseLockedResourceIfHeld()
        self.memory.assignment.phase = ControllerUpkeepPhase.UPGRADE
    }

    private suspend fun releaseLockedResourceIfHeld() {
        val resourceId = self.memory.lockedObjectId ?: return
        val roomName = self.memory.assignment.value?.roomName
            ?: selfOrNull?.room?.name
            ?: return

        val released: Boolean? = requestFrom(
            roomName,
            ReleaseResourceById(ownerId = id, resourceId = resourceId)
        )

        if (released == true) {
            setLockedResourceId(null)
        }
    }

//    private fun usedCapacityNow(): Int = self.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0
}
