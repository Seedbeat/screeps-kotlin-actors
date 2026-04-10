package room

import room.context.RoomConstructionSites
import room.context.RoomCreeps
import room.context.RoomStructuresInfo
import screeps.api.Room
import utils.lazyOnce

val Room.constructionSites: RoomConstructionSites by lazyOnce { RoomConstructionSites(this) }
val Room.structures: RoomStructuresInfo by lazyOnce { RoomStructuresInfo(this) }
val Room.creeps: RoomCreeps by lazyOnce { RoomCreeps(this) }
