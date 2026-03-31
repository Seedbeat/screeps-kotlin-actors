package room

import Root
import screeps.api.ConstructionSite
import screeps.api.FIND_HOSTILE_CONSTRUCTION_SITES
import screeps.api.FIND_MY_CONSTRUCTION_SITES
import screeps.api.Room
import utils.cache.CachedArrayInstance

class RoomConstructionSites(val room: Room) {
    private val cache = CachedArrayInstance<ConstructionSite>(lifetime = Root.LOCAL_TIME_MAX)

    val my: Array<ConstructionSite>
        get() = cache.getOrPut(firstKey = room.name, secondKey = FIND_MY_CONSTRUCTION_SITES) {
            room.find(findConstant = FIND_MY_CONSTRUCTION_SITES)
        }

    val hostile: Array<ConstructionSite>
        get() = cache.getOrPut(firstKey = room.name, secondKey = FIND_HOSTILE_CONSTRUCTION_SITES) {
            room.find(findConstant = FIND_HOSTILE_CONSTRUCTION_SITES)
        }
}