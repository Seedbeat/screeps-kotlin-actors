package utils.cache

import screeps.api.FindConstant
import screeps.api.Room

fun <T : V, V> CachedArrayInstance<V>.getByRoomFind(room: Room, findConstant: FindConstant<T>) =
    getOrPutTyped(firstKey = room.name, secondKey = findConstant) { room.find(findConstant = findConstant) }