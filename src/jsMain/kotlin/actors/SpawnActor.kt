package actors

import actors.SpawnCommand.EnsurePopulation
import actors.SpawnRequest.PopulationRequest
import actors.SpawnResponse.PopulationResponse
import actors.base.GameObjectBinding
import actors.base.IActorBinding
import memory.role
import screeps.api.FIND_MY_CREEPS
import screeps.api.Game
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
        is EnsurePopulation -> ensurePopulation(msg.role, msg.targetCount)
    }

    override suspend fun processRequest(msg: SpawnRequest): SpawnResponse<*> = when (msg) {
        is PopulationRequest -> {
            val count = currentPopulation(msg.role)
            PopulationResponse(count)
        }
    }

    private fun ensurePopulation(role: creep.enums.Role, targetCount: Int) {
        val spawn: StructureSpawn? = Game.getObjectById(id)
        if (spawn == null) {
            log.warn("Spawn object is missing for id=$id")
            return
        }

        if (spawn.spawning != null)
            return

        val count = currentPopulation(role)
        if (count >= targetCount)
            return

        val code = Spawner.spawn(spawn, role)
        if (code == OK) {
            log.info("Spawn request accepted: role=$role count=$count/$targetCount")
        }
    }

    private fun currentPopulation(role: creep.enums.Role): Int {
        val spawn: StructureSpawn? = Game.getObjectById(id)
        spawn ?: return 0
        return spawn.room.find(FIND_MY_CREEPS).count { creep -> creep.memory.role == role }
    }
}
