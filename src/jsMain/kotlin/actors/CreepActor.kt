package actors

import actors.CreepCommand.*
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

    private val intentService = CreepIntentService(api = this)

    override suspend fun processLifecycle(msg: Lifecycle) = when (msg) {
        is Bootstrap -> Unit
        is Tick -> intentService.executeAssignment()
    }

    override suspend fun processCommand(msg: CreepCommand) = when (msg) {
        is Assign -> intentService.assign(msg.assignment)
        is SetLockedResourceId -> intentService.setLockedResourceId(msg.resourceId)
        is ClearAssignment -> intentService.clearAssignmentState()
    }

    override suspend fun processRequest(msg: CreepRequest<*>): CreepResponse<*> = TODO()

    override fun onDestroy() {
        val creepMemory = selfOrNull?.memory ?: Memory.creeps[self.name] ?: return
        creepMemory.assignment = null
        creepMemory.lockedObjectId = null
    }
}
