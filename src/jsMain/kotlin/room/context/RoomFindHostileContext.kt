package room.context

import screeps.api.ConstructionSite
import screeps.api.FIND_MY_CONSTRUCTION_SITES
import screeps.api.Room
import screeps.api.RoomObject
import utils.cache.CachedArrayInstance
import utils.cache.getByRoomFind

class RoomFindHostileContext(val room: Room) {
    private val cache = CachedArrayInstance<RoomObject>()

    val constructionSites: Array<ConstructionSite>
        get() = cache.getByRoomFind(room, findConstant = FIND_MY_CONSTRUCTION_SITES)
}