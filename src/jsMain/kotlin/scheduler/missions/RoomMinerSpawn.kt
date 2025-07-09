package scheduler.missions

import creep.enums.Role
import memory.resourceSemaphore
import room.RoomContext
import room.RoomStage
import scheduler.IRoomDefaultMission
import utils.log.ILogging
import utils.log.Logging
import utils.maximum

object RoomMinerSpawn : IRoomDefaultMission, ILogging by Logging<RoomMinerSpawn>() {
    // Goals:
    // 1. just mine energy from source to container

    override fun RoomContext.execute() {
        if (creeps[Role.COURIER] < creeps[Role.MINER])
            return

        if (stage > RoomStage.Stage1 && sources.isNotEmpty()) {
            spawnMissionBase(Role.MINER) {
                sources.sumOf { room.memory.resourceSemaphore.maximum(it.id) ?: 0 }
            }
        }
    }
}