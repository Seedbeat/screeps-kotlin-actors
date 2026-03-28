package actors

import actors.RoomRequest.ReleaseResourceById
import actors.RoomRequest.TryAcquireResourceByType
import actors.assignments.CreepAssignmentPhase
import actors.base.ActorApi
import actors.base.ActorBinding
import creep.capabilities
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
        when (val assignment = self.memory.assignment) {
            null -> releaseLockedResourceIfHeld()
            is CreepAssignment.ControllerUpkeep -> executeControllerUpkeep(assignment)
            is CreepAssignment.Construction -> executeConstruction(assignment)
            is CreepAssignment.EnergyTransfer -> log.warn("EnergyKeep assignment is not implemented yet for creep $id")
        }
    }

    suspend fun assign(assignment: CreepAssignment) {
        replaceAssignment(assignment)
    }

    suspend fun clearAssignmentState(): Boolean {
        replaceAssignment(null)
        return true
    }

    fun setLockedResourceId(resourceId: String?) {
        self.memory.lockedObjectId = resourceId
    }

    fun status(): CreepStatus = CreepStatus(
        actorId = id,
        homeRoom = self.memory.homeRoom,
        currentRoom = self.room.name,
        assignment = self.memory.assignment,
        capabilities = self.capabilities,
        lockedResourceId = self.memory.lockedObjectId
    )

    private suspend fun executeControllerUpkeep(assignment: CreepAssignment.ControllerUpkeep) {
        val controller = resolveRoomObject<StructureController>(assignment, assignment.controllerId)

        if (controller == null) {
            clearAssignmentState()
            return
        }

        when (assignment.phase) {
            CreepAssignmentPhase.HARVEST -> executeHarvest(
                roomName = assignment.roomName,
                anchor = controller.pos,
                onHarvestDone = { changePhase(assignment, CreepAssignmentPhase.WORK) }
            )

            CreepAssignmentPhase.WORK -> executeEnergyWork(
                assignment = assignment,
                target = controller,
                action = { upgradeController(controller) },
                onWorkDone = { changePhase(assignment, CreepAssignmentPhase.HARVEST) }
            )
        }
    }

    private suspend fun executeConstruction(assignment: CreepAssignment.Construction) {
        val site = resolveRoomObject<ConstructionSite>(assignment, assignment.constructionSiteId)

        if (site == null) {
            clearAssignmentState()
            return
        }

        when (assignment.phase) {
            CreepAssignmentPhase.HARVEST -> executeHarvest(
                roomName = assignment.roomName,
                anchor = site.pos,
                onHarvestDone = { changePhase(assignment, CreepAssignmentPhase.WORK) }
            )

            CreepAssignmentPhase.WORK -> executeEnergyWork(
                assignment = assignment,
                target = site,
                action = { build(site) },
                onWorkDone = { changePhase(assignment, CreepAssignmentPhase.HARVEST) }
            )
        }
    }

//    private suspend fun executeEnergyKeep(assignment: CreepAssignment.EnergyTransfer) {
//        val site = resolveRoomObject<Structure>(assignment, assignment.targetId)
//
//        if (site == null) {
//            clearAssignmentState()
//            return
//        }
//
//        when (assignment.phase) {
//            CreepAssignmentPhase.HARVEST -> executeHarvest(
//                roomName = assignment.roomName,
//                anchor = site.pos,
//                onHarvestDone = { changePhase(assignment, CreepAssignmentPhase.WORK) }
//            )
//
//            CreepAssignmentPhase.WORK -> executeEnergyWork(
//                assignment = assignment,
//                target = site,
//                action = { transfer(site, RESOURCE_ENERGY) },
//                onWorkDone = { changePhase(assignment, CreepAssignmentPhase.HARVEST) }
//            )
//        }
//    }

    private suspend fun <T : RoomObject> executeEnergyWork(
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
        if (source == null) {
            if (self.energyStore.isNotEmpty) {
                onHarvestDone()
            }
            return
        }

        if (source.energy <= 0 && self.energyStore.isNotEmpty) {
            onHarvestDone()
            return
        }

        when (val code = self.harvest(source)) {
            OK -> {
                if (self.energyStore.isFull || (source.energy <= 0 && self.energyStore.isNotEmpty)) {
                    onHarvestDone()
                }
            }

            ERR_NOT_IN_RANGE -> self.moveTo(source)

            ERR_NOT_ENOUGH_RESOURCES -> {
                if (self.energyStore.isNotEmpty) {
                    onHarvestDone()
                }
            }

            ERR_INVALID_TARGET -> clearAssignmentState()

            else -> log.error("Controller upkeep harvest failed with code $code")
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
        val roomName = self.memory.assignment?.roomName
            ?: selfOrNull?.room?.name
            ?: return

        val released: Boolean? = requestFrom(
            actorId = roomName,
            payload = ReleaseResourceById(ownerId = id, resourceId = resourceId)
        )

        if (released != true) {
            log.warn("Failed to release lock $resourceId in room $roomName for creep $id: result=$released")
        }

        setLockedResourceId(null)
    }
}
