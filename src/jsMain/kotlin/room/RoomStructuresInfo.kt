package room

import screeps.api.FIND_HOSTILE_STRUCTURES
import screeps.api.FIND_MY_STRUCTURES
import screeps.api.Room

class RoomStructuresInfo(room: Room) {
    val my: RoomStructures = RoomStructures(room, FIND_MY_STRUCTURES)
    val hostile: RoomStructures = RoomStructures(room, FIND_HOSTILE_STRUCTURES)
}