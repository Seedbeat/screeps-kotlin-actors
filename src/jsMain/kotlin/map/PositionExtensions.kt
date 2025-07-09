package map

import screeps.api.*

fun RoomObjectNotNull.aroundObjects() = mapAround { x, y -> room.lookAt(x, y).toList() }.flatten()

fun HasPosition.openSidesCount(terrain: Room.Terrain) = mapAround { x, y -> terrain[x, y] }.count { it.isWalkable }

val TerrainMaskConstant.isWalkable: Boolean get() = this == TERRAIN_MASK_NONE || this == TERRAIN_MASK_SWAMP
val TerrainConstant.isWalkable: Boolean get() = this == TERRAIN_PLAIN || this == TERRAIN_SWAMP

fun <T> HasPosition.mapAround(action: (x: Int, y: Int) -> T?): List<T> {
    val xs = pos.x - 1
    val xe = pos.x + 1
    val ys = pos.y - 1
    val ye = pos.y + 1

    return (xs..xe).flatMap { x ->
        (ys..ye).mapNotNull { y ->
            if (x == pos.x && y == pos.y)
                return@mapNotNull null
            else
                action(x, y)
        }
    }
}