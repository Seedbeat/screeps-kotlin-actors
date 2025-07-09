package scheduler.missions

import creep.enums.Role
import room.RoomContext
import scheduler.IRoomDefaultMission
import utils.log.ILogging
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