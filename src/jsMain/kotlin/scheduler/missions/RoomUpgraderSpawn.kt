package scheduler.missions

import creep.enums.Role
import memory.workObjectId
import room.RoomContext
import room.RoomStage
import scheduler.IRoomDefaultMission
import utils.log.ILogging
import utils.log.Logging

object RoomUpgraderSpawn : IRoomDefaultMission, ILogging by Logging<RoomUpgraderSpawn>() {
    // Goals:
    // 1. al least two creeps should have upgrader role

    override fun RoomContext.execute() {
        if (sources.isNotEmpty() && room.controller != null) {
            spawnMissionBase(Role.UPGRADER, {
                when (stage) {
                    RoomStage.Stage1 -> 1
                    else -> if (isEnergyFull) 3 else 1
                }
            }) {
                workObjectId = room.controller!!.id
            }
        }
    }
}