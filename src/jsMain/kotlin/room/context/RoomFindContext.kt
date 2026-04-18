package room.context

import Root
import screeps.api.*
import utils.cache.CachedArrayInstance
import utils.cache.getByRoomFind

class RoomFindContext(val room: Room) {
    val my: RoomFindMyContext = RoomFindMyContext(room)
    val hostile: RoomFindHostileContext = RoomFindHostileContext(room)

    private val persistentCache = CachedArrayInstance<Identifiable>(lifetime = Root.LOCAL_TIME_MAX)
    private val volatileCache = CachedArrayInstance<Identifiable>()

    val sources: Array<Source>
        get() = persistentCache.getByRoomFind(room, findConstant = FIND_SOURCES)

    val minerals: Array<Mineral>
        get() = persistentCache.getByRoomFind(room, findConstant = FIND_MINERALS)

    val tombstones: Array<Tombstone>
        get() = volatileCache.getByRoomFind(room, findConstant = FIND_TOMBSTONES)

    val ruins: Array<Ruin>
        get() = volatileCache.getByRoomFind(room, findConstant = FIND_RUINS)

    val droppedResources: Array<Resource>
        get() = volatileCache.getByRoomFind(room, findConstant = FIND_DROPPED_RESOURCES)
}