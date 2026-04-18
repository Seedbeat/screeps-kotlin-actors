package room.context

import screeps.api.*
import utils.cache.CachedArrayInstance
import utils.cache.getByRoomFind

class RoomFindMyContext(val room: Room) {
    private val cache = CachedArrayInstance<RoomObject>()

    val constructionSites: Array<ConstructionSite>
        get() = cache.getByRoomFind(room, findConstant = FIND_MY_CONSTRUCTION_SITES)

    val creeps = RoomMyCreeps(room)
    val structures = RoomStructures(room, initial = FIND_MY_STRUCTURES)
}