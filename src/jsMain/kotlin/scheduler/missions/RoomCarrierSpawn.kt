package scheduler.missions

import creep.enums.Role
import memory.resourceSemaphore
import room.RoomContext
import room.RoomStage
import scheduler.IRoomDefaultMission
import scheduler.missions.RoomCourierSpawn.spawnMissionBase
import utils.log.ILogging
import utils.log.Logging
import utils.maximum

object RoomCarrierSpawn : IRoomDefaultMission, ILogging by Logging<RoomCarrierSpawn>() {
    // Goals:
    // 1. transport resources from storage to structures with energy lack

    override fun RoomContext.execute() {
        val minersCount = creeps[Role.MINER]

        if (minersCount < availableSourcePoints && creeps[Role.CARRIER] > minersCount)
            return

        if (stage >= RoomStage.Stage4 && sources.isNotEmpty()) {
            spawnMissionBase(Role.CARRIER) {
                availableSourcePoints * 2
            }
        }
    }
}