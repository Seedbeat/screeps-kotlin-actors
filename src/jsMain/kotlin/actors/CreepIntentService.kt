package actors

import actors.CreepAssignment.EnergyTransfer.Goal
import actors.RoomRequest.ReleaseResourceById
import actors.RoomRequest.TryAcquireResourceByType
import actors.assignments.CreepAssignmentPhase
import actors.base.ActorApi
import actors.base.ActorBinding
import memory.assignment
import memory.lockedObjectId
import screeps.api.*
import screeps.api.structures.Structure
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

    suspend fun executeAssignment() = when (val assignment = self.memory.assignment) {
        null -> releaseLockedResourceIfHeld()
        is CreepAssignment.ControllerUpkeep -> executeControllerUpkeep(assignment)
        is CreepAssignment.Construction -> executeConstruction(assignment)
        is CreepAssignment.EnergyTransfer -> executeEnergyTransfer(assignment)
    }

    suspend fun assign(assignment: CreepAssignment) {
        replaceAssignment(assignment)
    }

    suspend fun clearAssignmentState() {
        replaceAssignment(null)
    }

    fun setLockedResourceId(resourceId: String?) {
        self.memory.lockedObjectId = resourceId
    }

    private suspend fun executeControllerUpkeep(assignment: CreepAssignment.ControllerUpkeep) {
        val target = resolveRoomObject<StructureController>(assignment, assignment.controllerId)
            ?: return clearAssignmentState()

        executeHarvestBasedWork(assignment, target, action = Creep::upgradeController)
    }

    private suspend fun executeConstruction(assignment: CreepAssignment.Construction) {
        val target = resolveRoomObject<ConstructionSite>(assignment, assignment.constructionSiteId)
            ?: return clearAssignmentState()

        executeHarvestBasedWork(assignment, target, action = Creep::build)
    }

    private suspend fun executeEnergyTransfer(assignment: CreepAssignment.EnergyTransfer) {
        val structure = resolveRoomObject<Structure>(assignment, assignment.targetId)
            ?: return clearAssignmentState()

        executeHarvestBasedWork(assignment, structure) {
            val target = it.unsafeCast<StoreOwner>()
            when (assignment.goal) {
                is Goal.UntilFull -> transfer(target, RESOURCE_ENERGY)
                is Goal.Amount -> transfer(target, RESOURCE_ENERGY, assignment.goal.amount)

                is Goal.Percent ->
                    if (target.energyStore.percentage < assignment.goal.percentage)
                        transfer(target, RESOURCE_ENERGY)
                    else
                        ERR_INVALID_TARGET

            }
        }
    }

    private suspend fun <T> executeHarvestBasedWork(
        assignment: CreepAssignment.PhaseAssignment,
        target: T,
        action: suspend Creep.(T) -> ScreepsReturnCode
    ) where T : HasPosition, T : Identifiable = when (assignment.phase) {
        CreepAssignmentPhase.HARVEST -> executeHarvest(
            roomName = assignment.roomName,
            anchor = target.pos,
            onHarvestDone = { changePhase(assignment, CreepAssignmentPhase.WORK) }
        )

        CreepAssignmentPhase.WORK -> executeEnergyWork(
            assignment = assignment,
            target = target,
            action = { action(target) },
            onWorkDone = { changePhase(assignment, CreepAssignmentPhase.HARVEST) }
        )
    }

    private suspend fun <T : HasPosition> executeEnergyWork(
        assignment: CreepAssignment,
        target: T,
        action: suspend Creep.() -> ScreepsReturnCode,
        onWorkDone: suspend () -> Unit
    ) {
        if (self.energyStore.isEmpty) {
            onWorkDone()
            return
        }

        releaseLockedResourceIfHeld()

        when (val code = self.action()) {
            OK -> Unit
            ERR_NOT_IN_RANGE -> self.moveTo(target)
            ERR_NOT_ENOUGH_RESOURCES -> onWorkDone()
            ERR_FULL,
            ERR_INVALID_TARGET -> clearAssignmentState()
            else -> log.error("${assignment::class.simpleName} failed with code $code")
        }
    }

    private suspend fun executeHarvest(
        roomName: String,
        anchor: RoomPosition,
        onHarvestDone: suspend () -> Unit
    ) {
        if (self.energyStore.isFull) {
            onHarvestDone()
            return
        }

        val source = resolveHarvestSource(roomName, anchor)
        if (source == null || source.energy == 0) {
            if (self.energyStore.isNotEmpty) {
                onHarvestDone()
            }
            return
        }

        when (val code = self.harvest(source)) {
            OK -> Unit
            ERR_NOT_IN_RANGE -> self.moveTo(source)
            ERR_NOT_ENOUGH_RESOURCES -> Unit
            ERR_INVALID_TARGET -> clearAssignmentState()
            else -> log.error("Harvest failed with code $code")
        }
    }

    private suspend fun resolveHarvestSource(roomName: String, anchor: RoomPosition): Source? {
        val lockedResourceId = self.memory.lockedObjectId
        val lockedSource = lockedResourceId?.let { Game.getObjectById<Source>(it) }

        if (lockedSource != null && lockedSource.energy > 0) {
            return lockedSource
        }

        if (lockedResourceId != null) {
            releaseLockedResourceIfHeld()
        }

        val acquiredResourceId = ensurePreferredResourceLock(roomName, anchor) ?: return null
        return Game.getObjectById(acquiredResourceId)
    }

    private suspend fun ensurePreferredResourceLock(roomName: String, anchor: RoomPosition): String? {
        val lockedResourceId = self.memory.lockedObjectId
        if (lockedResourceId != null) {
            return lockedResourceId
        }

        val acquiredResourceId: String? = requestFrom(
            actorId = roomName,
            payload = TryAcquireResourceByType(
                ownerId = id,
                near = anchor,
                type = RoomResourceType.SOURCE
            )
        )

        setLockedResourceId(acquiredResourceId)

        return acquiredResourceId
    }

    private suspend fun replaceAssignment(assignment: CreepAssignment?) {
        releaseLockedResourceIfHeld()
        self.memory.assignment = assignment
    }

    private suspend fun changePhase(
        assignment: CreepAssignment.PhaseAssignment,
        phase: CreepAssignmentPhase
    ) {
        releaseLockedResourceIfHeld()
        self.memory.assignment = assignment.withPhase(phase)
    }

    private fun <T> resolveRoomObject(
        assignment: CreepAssignment,
        id: String
    ): T? where T : HasPosition, T : Identifiable {

        val obj = Game.getObjectById<T>(id)
            ?: return null

        val objRoomName = obj.pos.roomName
        if (objRoomName != assignment.roomName) {
            log.error(
                "Target object assignment room mismatch: " +
                        "assignmentRoom=${assignment.roomName}, " +
                        "objectRoom=$objRoomName, " +
                        "objectId=${id}"
            )
            return null
        }

        return obj
    }

    private suspend fun releaseLockedResourceIfHeld() {
        val resourceId = self.memory.lockedObjectId ?: return
        val roomActorId = self.memory.assignment?.roomName
            ?: selfOrNull?.room?.name
            ?: return

        val released: Boolean? = ReleaseResourceById(
            ownerId = id,
            resourceId = resourceId
        ).requestFrom(roomActorId)

        if (released != true) {
            log.warn("Failed to release lock $resourceId in room $roomActorId for creep $id: result=$released")
        }

        setLockedResourceId(null)
    }
}
