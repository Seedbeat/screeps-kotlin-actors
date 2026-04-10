package room

import screeps.api.Room
import utils.lazyOnce

val Room.constructionSites: RoomConstructionSites by lazyOnce { RoomConstructionSites(this) }
val Room.structures: RoomStructuresInfo by lazyOnce { RoomStructuresInfo(this) }
val Room.creeps: RoomCreeps by lazyOnce { RoomCreeps(this) }
