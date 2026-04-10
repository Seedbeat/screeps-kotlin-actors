package spawn

import actors.ActorBase
import actors.base.ActorBinding
import actors.base.GameObjectBinding
import screeps.api.OK
import screeps.api.structures.StructureSpawn
import spawn.Spawner.spawn
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class SpawnActor(
    id: String
) : ActorBase<StructureSpawn, SpawnCommand, SpawnRequest<*>, SpawnResponse<*>>(id),
    ActorBinding<StructureSpawn> by GameObjectBinding(id),
    ILogging by Logging<SpawnActor>(id, LogLevel.INFO) {

    override suspend fun processCommand(msg: SpawnCommand) = when (msg) {
        is SpawnCommand.TrySpawnWorker -> trySpawnWorker(msg)
    }

    override suspend fun processRequest(msg: SpawnRequest<*>): SpawnResponse<*> = TODO()

    private fun trySpawnWorker(msg: SpawnCommand.TrySpawnWorker) {
        if (self.spawning != null)
            return

        val code = self.spawn(
            assignment = msg.assignment,
            profile = msg.profile
        )
        if (code == OK) {
            log.info("Spawn request accepted: $msg")
        } else {
            log.info("Spawn request rejected: $msg")
        }
    }
}
