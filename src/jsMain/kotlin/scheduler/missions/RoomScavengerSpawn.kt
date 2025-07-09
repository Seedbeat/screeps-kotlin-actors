package scheduler.missions

import creep.enums.Role
import memory.resourceSemaphore
import room.RoomContext
import room.RoomStage
import scheduler.IRoomDefaultMission
import utils.log.ILogging
import utils.log.Logging
import utils.maximum

object RoomScavengerSpawn : IRoomDefaultMission, ILogging by Logging<RoomScavengerSpawn>() {
    // Goals:
    // 1. cleanup map from any dropped resources

    override fun RoomContext.execute() {
        if (stage > RoomStage.Stage3 && droppedResources.any { it.amount >= 100 }) {
            spawnMissionBase(Role.SCAVENGER) { 1 }
        }
    }
}