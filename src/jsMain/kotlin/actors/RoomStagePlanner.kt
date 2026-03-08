package actors

import map.mapAround
import room.RoomStage
import screeps.api.*
import screeps.api.structures.StructureContainer

class RoomStagePlanner(
    private val room: Room,
) {
    fun calculate(): RoomStage {
        val controllerLevel = room.controller?.level ?: 0
        val myStructures = room.find(FIND_MY_STRUCTURES)

        val extensionCount = myStructures.count { it.structureType == STRUCTURE_EXTENSION }
        val towerCount = myStructures.count { it.structureType == STRUCTURE_TOWER }

        val hasAllSourceContainers = hasAllSourcesAdjacentToContainers()
        val hasStorage = room.storage != null

        return when {
            controllerLevel >= 4 && extensionCount >= 20 && towerCount >= 1 && hasAllSourceContainers && hasStorage ->
                RoomStage.Stage4

            controllerLevel >= 3 && extensionCount >= 10 && towerCount >= 1 && hasAllSourceContainers ->
                RoomStage.Stage3

            controllerLevel >= 2 && extensionCount >= 5 && hasAllSourceContainers ->
                RoomStage.Stage2

            else -> RoomStage.Stage1
        }
    }

    private fun hasAllSourcesAdjacentToContainers(): Boolean {
        val sources = room.find(FIND_SOURCES)
        if (sources.isEmpty()) {
            return false
        }

        val containersByPos = room.find(FIND_STRUCTURES)
            .asSequence()
            .filter { structure -> structure.structureType == STRUCTURE_CONTAINER }
            .map { structure -> structure.unsafeCast<StructureContainer>() }
            .associateBy { container -> container.pos.x to container.pos.y }

        return sources.all { source ->
            source.mapAround { x, y -> containersByPos[x to y] }.any()
        }
    }
}
