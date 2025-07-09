package scheduler.missions

import creep.enums.Role
import room.RoomContext
import room.RoomStage
import scheduler.IRoomDefaultMission
import screeps.api.RESOURCE_ENERGY
import utils.log.ILogging
import utils.log.Logging

object RoomCourierSpawn : IRoomDefaultMission, ILogging by Logging<RoomCourierSpawn>() {
    // Goals:
    // 1. transport resources from containers to structures with energy lack

    override fun RoomContext.execute() {
        val minersCount = creeps[Role.MINER]

        if (minersCount < availableSourcePoints && creeps[Role.COURIER] > minersCount)
            return

        if (stage > RoomStage.Stage1 && sources.isNotEmpty()) {
            spawnMissionBase(Role.COURIER) {
//                availableSourcePoints

                if (stage >= RoomStage.Stage4 && (room.storage?.store?.getUsedCapacity(RESOURCE_ENERGY) ?: 0) > 5000)
                    availableSourcePoints * 2
                else
                    availableSourcePoints
            }
        }
    }
}