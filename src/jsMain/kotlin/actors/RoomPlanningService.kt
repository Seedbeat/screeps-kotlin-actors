package actors

import actor.ActorSystem
import actors.SystemRequest.QueryCreeps
import actors.base.ActorApi
import actors.base.ActorBinding
import actors.base.IntentResultType
import screeps.api.FIND_MY_SPAWNS
import screeps.api.Room
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class RoomPlanningService<T>(
    api: T
) : ActorApi by api,
    ActorBinding<Room> by api,
    ILogging by Logging<RoomPlanningService<T>>(api.self.name, LogLevel.INFO)
        where T : ActorApi,
              T : ActorBinding<Room> {
    suspend fun ensureControllerSurvival(): IntentResultType {
        val controller = self.controller
            ?: return IntentResultType.RETAINED

        val creeps = systemRequest(payload = QueryCreeps(homeRoom = self.name))

        val assignedSurvivalCreep = creeps.firstOrNull { creep ->
            val assignment = creep.assignment as? CreepAssignment.ControllerUpkeep
            assignment?.roomName == self.name && assignment.controllerId == controller.id
        }

        if (assignedSurvivalCreep != null) {
            return IntentResultType.COMPLETED
        }

        val existingSurvivalCreep = creeps.firstOrNull { creep ->
            creep.capabilities.canDoControllerUpkeep && creep.assignment == null
        }

        if (existingSurvivalCreep != null) {
            sendTo(
                existingSurvivalCreep.actorId,
                payload = CreepCommand.Assign(
                    assignment = CreepAssignment.ControllerUpkeep(
                        roomName = self.name,
                        controllerId = controller.id
                    )
                )
            )
            return IntentResultType.COMPLETED
        }

        val availableSpawnActorId = self.find(FIND_MY_SPAWNS)
            .firstOrNull { spawn -> spawn.spawning == null && ActorSystem.contains(spawn.id) }
            ?.id
            ?: return IntentResultType.RETAINED

        sendTo(
            availableSpawnActorId,
            payload = SpawnCommand.TrySpawnControllerSurvivalWorker(
                roomName = self.name,
                controllerId = controller.id
            )
        )
        return IntentResultType.COMPLETED
    }
}
