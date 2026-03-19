package scheduler.missions

import creep.enums.Role
import memory.resourceSemaphore
import room.RoomContext
import room.RoomStage
import scheduler.IRoomDefaultMission
import screeps.api.RESOURCE_ENERGY
import store.fillPercentage
import utils.log.ILogging
import utils.log.Logging

object RoomHarvesterSpawn : IRoomDefaultMission, ILogging by Logging<RoomHarvesterSpawn>() {
    // Goals:
    // 1. harvest some energy for spawns

    override fun RoomContext.execute() {
        if (isEnergyFull || stage > RoomStage.Stage1 && creeps[Role.MINER] > 0 &&
            (creeps[Role.COURIER] > 0 || sourcesContainers.any { it.store.fillPercentage(RESOURCE_ENERGY) >= 20 })
        ) {
            return
        }

        if (sources.isNotEmpty()) {
            spawnMissionBase(Role.HARVESTER) {
                sources.sumOf { room.memory.resourceSemaphore.maximum(it.id) ?: 0 }
            }
        }
    }
}
