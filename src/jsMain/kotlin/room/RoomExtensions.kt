package room

import room.context.RoomFindContext
import screeps.api.Room
import utils.lazyOnce

val Room.find: RoomFindContext by lazyOnce { RoomFindContext(this) }
