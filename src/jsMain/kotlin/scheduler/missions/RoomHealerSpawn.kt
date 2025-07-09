package scheduler.missions

import creep.enums.Role
import scheduler.IRoomDefaultMission
import room.RoomContext
import screeps.api.RESOURCE_ENERGY
import store.fillPercentage
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import kotlin.math.max

object RoomHealerSpawn : IRoomDefaultMission, ILogging by Logging<RoomHealerSpawn>() {
    override fun RoomContext.execute() {
        if (damagedCreeps.isNotEmpty()) {
            spawnMissionBase(Role.HEALER) {
                if (isEnergyFull)
                    max(damagedCreeps.count() / 4, 2)
                else
                    1
            }
        }
    }
}