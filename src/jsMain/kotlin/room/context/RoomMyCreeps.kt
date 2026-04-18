package room.context

import screeps.api.*
import utils.cache.CachedArrayInstance
import utils.cache.getByRoomFind

class RoomMyCreeps(val room: Room) {
    private val cache = CachedArrayInstance<GenericCreep>()

    val creeps: Array<Creep>
        get() = cache.getByRoomFind(room, findConstant = FIND_MY_CREEPS)

    val powerCreeps: Array<PowerCreep>
        get() = cache.getByRoomFind(room, findConstant = FIND_MY_POWER_CREEPS)
}