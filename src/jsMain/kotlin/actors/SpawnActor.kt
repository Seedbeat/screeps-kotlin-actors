package actors

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
) : ActorBase<StructureSpawn, SpawnCommand, SpawnRequest<*>, SpawnResponse<*>>(id),
    ActorBinding<StructureSpawn> by GameObjectBinding(id),
    ILogging by Logging<SpawnActor>(id, LogLevel.INFO) {

    override suspend fun processCommand(msg: SpawnCommand) = when (msg) {
        is SpawnCommand.TrySpawnControllerSurvivalWorker -> trySpawnControllerSurvivalWorker(msg)
        is SpawnCommand.TrySpawnConstructionWorker -> trySpawnConstructionWorker(msg)
    }

    override suspend fun processRequest(msg: SpawnRequest<*>): SpawnResponse<*> = TODO()

    private fun trySpawnControllerSurvivalWorker(msg: SpawnCommand.TrySpawnControllerSurvivalWorker) {
        trySpawnAssignment(
            assignment = CreepAssignment.ControllerUpkeep(
                roomName = msg.roomName,
                controllerId = msg.controllerId
            ),
            msg = msg
        )
    }

    private fun trySpawnConstructionWorker(msg: SpawnCommand.TrySpawnConstructionWorker) {
        trySpawnAssignment(
            assignment = CreepAssignment.Construction(
                roomName = msg.roomName,
                constructionSiteId = msg.constructionSiteId
            ),
            msg = msg
        )
    }

    private fun trySpawnAssignment(assignment: CreepAssignment, msg: SpawnCommand) {
        if (self.spawning != null)
            return

        val code = self.spawn(assignment = assignment)
        if (code == OK) {
            log.info("Spawn request accepted: $msg")
        } else {
            log.info("Spawn request rejected: $msg")
        }
    }
}
