package actors

import actors.SpawnRequest.PopulationRequest
import actors.SpawnResponse.PopulationResponse
import actors.SystemRequest.CountCreeps
import actors.base.ActorBinding
import actors.base.GameObjectBinding
import screeps.api.OK
import screeps.api.structures.StructureSpawn
import spawn.SpawnerNew.spawn
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class SpawnActor(
    id: String
) : ActorBase<StructureSpawn, SpawnCommand, SpawnRequest, SpawnResponse<*>>(id),
    ActorBinding<StructureSpawn> by GameObjectBinding(id),
    ILogging by Logging.Companion<SpawnActor>(id, LogLevel.INFO) {

    override suspend fun processCommand(msg: SpawnCommand) = when (msg) {
        is SpawnCommand.TrySpawnControllerSurvivalWorker -> trySpawnControllerSurvivalWorker(msg)
    }

    override suspend fun processRequest(msg: SpawnRequest): SpawnResponse<*> = when (msg) {
        is PopulationRequest -> {
            val count: Int = systemRequest(
                CountCreeps(homeRoom = self.room.name, role = msg.role)
            )
            PopulationResponse(count)
        }
    }

    private fun trySpawnControllerSurvivalWorker(msg: SpawnCommand.TrySpawnControllerSurvivalWorker) {
        if (self.spawning != null)
            return

        val code = self.spawn(
            assignment = CreepAssignment.ControllerUpkeep(
                roomName = msg.roomName,
                controllerId = msg.controllerId,
                sourceId = msg.sourceId
            )
        )
        if (code == OK) {
            log.info("Spawn request accepted: $msg")
        } else {
            log.info("Spawn request rejected: $msg")
        }
    }
}
