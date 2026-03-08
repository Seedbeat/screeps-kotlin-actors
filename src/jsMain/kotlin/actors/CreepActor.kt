package actors

import actors.CreepCommand.SetLockedResourceId
import actors.CreepRequest.StatusRequest
import actors.CreepResponse.StatusResponse
import actors.CreepResponse.UnassignResponse
import actors.base.GameCreepBinding
import actors.base.IActorBinding
import creep.enums.Role
import creep.enums.State
import memory.lockedObjectId
import memory.role
import memory.state
import memory.workObjectId
import screeps.api.Creep
import screeps.api.Memory
import screeps.api.get
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class CreepActor(
    id: String
) : ActorBase<Creep, CreepCommand, CreepRequest, CreepResponse<*>>(id),
    IActorBinding<Creep> by GameCreepBinding(id),
    ILogging by Logging<CreepActor>(id, LogLevel.INFO) {

    override suspend fun processCommand(msg: CreepCommand) = when (msg) {
        CreepCommand.Noop -> Unit
        is SetLockedResourceId -> setLockedResourceId(msg.resourceId)
    }

    override suspend fun processRequest(msg: CreepRequest): CreepResponse<*> = when (msg) {
        StatusRequest -> StatusResponse(result = "creep=$id")
        CreepRequest.Unassign -> UnassignResponse(result = clearAssignmentState())
    }

    override fun onDestroy() {
        clearDestroyedAssignmentState()
    }

    private fun clearAssignmentState(): Boolean {
        val creepMemory = self.memory

        if (creepMemory.role != Role.UPGRADER) {
            creepMemory.workObjectId = ""
        }

        creepMemory.state = State.UNASSIGNED
        setLockedResourceId(null)
        return true
    }

    private fun setLockedResourceId(resourceId: String?) {
        val creepMemory = self.memory
        creepMemory.lockedObjectId = resourceId ?: ""
    }

    private fun clearDestroyedAssignmentState() {
        val creepMemory = selfOrNull?.memory ?: Memory.creeps[id] ?: return

        if (creepMemory.role != Role.UPGRADER) {
            creepMemory.workObjectId = ""
        }

        creepMemory.state = State.UNASSIGNED
        creepMemory.lockedObjectId = ""
    }
}
