package actors

import actors.CreepCommand.*
import actors.CreepRequest.StatusRequest
import actors.CreepResponse.StatusResponse
import actors.CreepResponse.UnassignResponse
import actors.base.ActorBinding
import actors.base.GameCreepBinding
import actors.base.Lifecycle
import screeps.api.Creep
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
        is Lifecycle.Bootstrap -> Unit
        is Lifecycle.Tick -> intentService.executeAssignment()
    }

    override suspend fun processCommand(msg: CreepCommand) = when (msg) {
        Noop -> Unit
        is Assign -> intentService.assign(msg.assignment)
        is SetLockedResourceId -> intentService.setLockedResourceId(msg.resourceId)
        ClearAssignment -> {
            intentService.clearAssignmentState()
            Unit
        }
    }

    override suspend fun processRequest(msg: CreepRequest<*>): CreepResponse<*> = when (msg) {
        StatusRequest -> StatusResponse(result = intentService.status())
        CreepRequest.Unassign -> UnassignResponse(result = intentService.clearAssignmentState())
    }

    override fun onDestroy() {
        intentService.clearDestroyedAssignmentState()
    }
}
