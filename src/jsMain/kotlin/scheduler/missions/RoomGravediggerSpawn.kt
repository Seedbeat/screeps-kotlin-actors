package scheduler.missions

import creep.enums.Role
import room.RoomContext
import room.RoomStage
import scheduler.IRoomDefaultMission
import utils.log.ILogging
import utils.log.Logging

object RoomGravediggerSpawn : IRoomDefaultMission, ILogging by Logging<RoomGravediggerSpawn>() {
    // Goals:
    // 1. cleanup map from any dropped resources

    override fun RoomContext.execute() {
        if (stage > RoomStage.Stage3 && tombstoneResources.isNotEmpty()) {
            spawnMissionBase(Role.GRAVEDIGGER) { 1 }
        }
    }
}