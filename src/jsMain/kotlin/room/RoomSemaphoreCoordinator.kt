package room

import map.aroundObjects
import map.isWalkable
import map.mapAround
import map.openSidesCount
import memory.resourceSemaphore
import memory.stage
import room.enums.RoomStage
import screeps.api.*
import screeps.api.structures.StructureContainer
import utils.log.ILogging
import utils.log.Logging

class RoomSemaphoreCoordinator(
    private val room: Room,
) : ILogging by Logging<RoomSemaphoreCoordinator>(room.name) {

    data class Definition(
        val id: String,
        val max: Int,
    )

    data class SyncPlan(
        val toCreate: List<Definition>,
        val toRecreate: List<Definition>,
        val toDelete: Set<String>,
        val changedResourceIds: Set<String>,
    )

    fun createSyncPlan(): SyncPlan {
        val definitionsById = collectDefinitions().associateBy(Definition::id)
        val semaphore = room.memory.resourceSemaphore
        val existingIds = semaphore.keys.toSet()

        val toCreate = mutableListOf<Definition>()
        val toRecreate = mutableListOf<Definition>()

        definitionsById.values.forEach { definition ->
            val currentMax = semaphore.maximum(definition.id)
            when {
                currentMax == null -> toCreate += definition
                currentMax != definition.max -> toRecreate += definition
            }
        }

        val toDelete = existingIds - definitionsById.keys
        val changedResourceIds = buildSet {
            addAll(toDelete)
            addAll(toRecreate.map { it.id })
        }
        return SyncPlan(
            toCreate = toCreate,
            toRecreate = toRecreate,
            toDelete = toDelete,
            changedResourceIds = changedResourceIds,
        )
    }

    private fun collectDefinitions(): List<Definition> {
        val terrain = room.getTerrain()
        val sources = room.find(FIND_SOURCES)
        val definitions = mutableListOf<Definition>()

        sources.forEach { source ->
            definitions += Definition(source.id, source.openSidesCount(terrain))
        }

        if (room.memory.stage > RoomStage.Stage1) {
            findSourceAdjacentContainers(sources).forEach { container ->
                val openSides = calculateContainerOpenSides(container)
                definitions += Definition(container.id, openSides)
            }
        }

        room.storage?.let { storage ->
            definitions += Definition(storage.id, 8)
        }

        return definitions
    }

    private fun calculateContainerOpenSides(container: StructureContainer): Int {
        val around = container.aroundObjects()
        val walkableAround = around.count { it.type == LOOK_TERRAIN && it.terrain!!.isWalkable }
        val containersAround = around.count { it.type == LOOK_STRUCTURES && it.structure!!.structureType == STRUCTURE_CONTAINER }
        return maxOf(walkableAround - containersAround, 1)
    }

    private fun findSourceAdjacentContainers(sources: Array<Source>): List<StructureContainer> {
        val containersByPos = room.find(FIND_STRUCTURES)
            .asSequence()
            .filter { structure -> structure.structureType == STRUCTURE_CONTAINER }
            .map { structure -> structure.unsafeCast<StructureContainer>() }
            .associateBy { container -> container.pos.x to container.pos.y }

        return sources
            .asSequence()
            .flatMap { source ->
                source.mapAround { x, y -> containersByPos[x to y] }.asSequence()
            }
            .distinctBy { container -> container.id }
            .toList()
    }
}
