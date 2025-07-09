package scheduler.missions

import creep.enums.Role
import room.RoomContext
import scheduler.IRoomDefaultMission
import screeps.api.RESOURCE_ENERGY
import store.fillPercentage
import utils.log.ILogging
import utils.log.Logging
import kotlin.math.max

object RoomKnightSpawn : IRoomDefaultMission, ILogging by Logging<RoomKnightSpawn>() {
    override fun RoomContext.execute() {
        if (hostileCreeps.isNotEmpty()) {
            spawnMissionBase(Role.KNIGHT) {
                if (isEnergyFull && sourcesContainers.any { it.store.fillPercentage(RESOURCE_ENERGY) >= 25 })
                    max(hostileCreeps.count(), 2)
                else
                    2
            }
        }
    }
}