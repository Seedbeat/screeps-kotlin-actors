package creep

import actors.ActorBase
import actors.base.ActorBinding
import actors.base.GameCreepBinding
import actors.base.Lifecycle
import actors.base.Lifecycle.Bootstrap
import actors.base.Lifecycle.Tick
import memory.assignment
import memory.lockedObjectId
import screeps.api.Creep
import screeps.api.Memory
import screeps.api.get
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class CreepActor(
    id: String
) : ActorBase<Creep, CreepCommand, CreepRequest<*>, CreepResponse<*>>(id),
    ActorBinding<Creep> by GameCreepBinding(id),
    ILogging by Logging<CreepActor>(id, LogLevel.INFO) {

    private val intentService = CreepAssignmentService(api = this)

    override suspend fun processLifecycle(msg: Lifecycle) = when (msg) {
        is Bootstrap -> Unit
        is Tick -> intentService.executeAssignment()
    }

    override suspend fun processCommand(msg: CreepCommand) = when (msg) {
        is CreepCommand.Assign -> intentService.assign(msg.assignment)
        is CreepCommand.SetLockedResourceId -> intentService.setLockedResourceId(msg.resourceId)
        is CreepCommand.ClearAssignment -> intentService.clearAssignmentState()
    }

    override suspend fun processRequest(msg: CreepRequest<*>): CreepResponse<*> = TODO()

    override fun onDestroy() {
        val creepMemory = selfOrNull?.memory ?: Memory.creeps[self.name] ?: return
        creepMemory.assignment = null
        creepMemory.lockedObjectId = null
    }
}
