package room.context

import screeps.api.Creep
import screeps.api.FIND_HOSTILE_CREEPS
import screeps.api.FIND_MY_CREEPS
import screeps.api.Room
import utils.cache.CachedArrayInstance

class RoomCreeps(val room: Room) {
    private val cache = CachedArrayInstance<Creep>()

    val my: Array<Creep>
        get() = cache.getOrPut(firstKey = room.name, secondKey = FIND_MY_CREEPS) {
            room.find(findConstant = FIND_MY_CREEPS)
        }

    val hostile: Array<Creep>
        get() = cache.getOrPut(firstKey = room.name, secondKey = FIND_HOSTILE_CREEPS) {
            room.find(findConstant = FIND_HOSTILE_CREEPS)
        }
}