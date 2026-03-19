package actors

import actors.SpawnRequest.PopulationRequest
import actors.SpawnResponse.PopulationResponse
import actors.SystemRequest.CountCreeps
import actors.base.ActorBinding
import actors.base.Actors
import actors.base.GameObjectBinding
import creep.enums.Role
import memory.assignment
import screeps.api.CreepMemory
import screeps.api.OK
import screeps.api.structures.StructureSpawn
import spawn.Spawner
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
            val count: Int = requestFrom(
                Actors.SYSTEM,
                CountCreeps(homeRoom = self.room.name, role = msg.role)
            )
            PopulationResponse(count)
        }
    }

    private fun trySpawn(role: Role, memory: CreepMemory.() -> Unit = {}) {
        if (self.spawning != null)
            return

        val code = Spawner.spawn(self, role, memory = memory)
        if (code == OK) {
            log.info("Spawn request accepted: role=$role homeRoom=${self.room.name}")
        }
    }

    private fun trySpawnControllerSurvivalWorker(msg: SpawnCommand.TrySpawnControllerSurvivalWorker) {
        trySpawn(Role.HARVESTER) {
            assignment.value = CreepAssignment.ControllerUpkeep(
                roomName = msg.roomName,
                controllerId = msg.controllerId,
                sourceId = msg.sourceId
            )
        }
    }
}
