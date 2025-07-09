package scheduler.missions

import creep.enums.Role
import memory.workObjectId
import room.RoomContext
import scheduler.IRoomDefaultMission
import utils.log.ILogging
import utils.log.Logging

object RoomSafe : IRoomDefaultMission, ILogging by Logging<RoomSafe>() {
    // Goals:
    // 1. upgrade controller -> al least one creep should have upgrader role

    override fun RoomContext.execute() {
        if (sources.isNotEmpty() && room.controller != null) {
            spawnMissionBase(Role.UPGRADER, { 1 }) {
                workObjectId = room.controller!!.id
            }
        }
    }
}