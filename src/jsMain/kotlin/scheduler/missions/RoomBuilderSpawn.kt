package scheduler.missions

import creep.enums.Role
import scheduler.IRoomDefaultMission
import room.RoomContext
import screeps.api.RESOURCE_ENERGY
import store.fillPercentage
import utils.log.ILogging
import utils.log.Logging
import kotlin.math.max

object RoomBuilderSpawn : IRoomDefaultMission, ILogging by Logging<RoomBuilderSpawn>() {
    // Goals:
    // 1. create at least 2 builders if we need to construct something

    override fun RoomContext.execute() {
        if (!isSourcePresent)
            return

        if (constructionSites.isNotEmpty()) {
            spawnMissionBase(Role.BUILDER) {
                if (isEnergyFull && sourcesContainers.any { it.store.fillPercentage(RESOURCE_ENERGY) >= 50 })
                    max(constructionSites.count() / 4, 2)
                else
                    2
            }
        }
    }
}