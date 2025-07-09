package scheduler.missions

import creep.enums.Role
import map.health
import room.RoomContext
import scheduler.IRoomDefaultMission
import screeps.api.RESOURCE_ENERGY
import store.fillPercentage
import utils.log.ILogging
import utils.log.Logging
import kotlin.math.max

object RoomRepairerSpawn : IRoomDefaultMission, ILogging by Logging<RoomRepairerSpawn>() {
    // Goals:
    // 1. repair any damaged structure (health < 80%)

    override fun RoomContext.execute() {
        if (!isSourcePresent)
            return

        val damagedStructuresCount = damagedStructures.count { it.health <= 80 }
        if (damagedStructuresCount > 0) {
            spawnMissionBase(Role.REPAIRER) {
                if (isEnergyFull && sourcesContainers.any { it.store.fillPercentage(RESOURCE_ENERGY) >= 50 })
                    max(damagedStructuresCount / 10, 2)
                else
                    2
            }
        }
    }
}