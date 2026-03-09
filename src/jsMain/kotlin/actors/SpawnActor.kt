package actors

import actors.SpawnRequest.PopulationRequest
import actors.SpawnResponse.PopulationResponse
import actors.SystemRequest.CountCreeps
import actors.base.GameObjectBinding
import actors.base.IActorBinding
import creep.enums.Role
import screeps.api.OK
import screeps.api.structures.StructureSpawn
import spawn.Spawner
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class SpawnActor(
    id: String
) : ActorBase<StructureSpawn, SpawnCommand, SpawnRequest, SpawnResponse<*>>(id),
    IActorBinding<StructureSpawn> by GameObjectBinding(id),
    ILogging by Logging.Companion<SpawnActor>(id, LogLevel.INFO) {

    override suspend fun processCommand(msg: SpawnCommand) = when (msg) {
        is SpawnCommand.TrySpawn -> trySpawn(msg.role)
    }

    override suspend fun processRequest(msg: SpawnRequest): SpawnResponse<*> = when (msg) {
        is PopulationRequest -> {
            val count: Int = requestFrom(
                SystemActor.SYSTEM,
                CountCreeps(homeRoom = self.room.name, role = msg.role)
            )
            PopulationResponse(count)
        }
    }

    private fun trySpawn(role: Role) {
        if (self.spawning != null)
            return

        val code = Spawner.spawn(self, role)
        if (code == OK) {
            log.info("Spawn request accepted: role=$role homeRoom=${self.room.name}")
        }
    }
}
