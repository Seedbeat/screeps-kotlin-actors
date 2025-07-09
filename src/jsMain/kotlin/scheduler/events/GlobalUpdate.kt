package scheduler.events

import map.aroundObjects
import map.isWalkable
import map.openSidesCount
import memory.isUpdateNeed
import room.RoomContext
import room.RoomStage
import scheduler.IEvent
import screeps.api.*
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

object GlobalUpdate : IEvent, ILogging by Logging<GlobalUpdate>(LogLevel.INFO) {
    override fun execute() {
        if (Memory.isUpdateNeed) {
            try {
                update()
            } finally {
                Memory.isUpdateNeed = false
            }
        }
    }

    private fun update() {
        Game.rooms.values.forEach {
            val context = Root.room(it.name)
            updateAvailableResources(context)
        }
    }

    private fun updateAvailableResources(context: RoomContext) = context.run {
        val terrain = room.getTerrain()

        sources.map { source ->
            val openSides = source.openSidesCount(terrain)

            this@GlobalUpdate.log.info("${source.id}: open sides = $openSides")
            initializeSemaphore(source.id, openSides)
        }

        if (context.stage > RoomStage.Stage1) {
            sourcesContainers.map { container ->
                val around = container.aroundObjects()

                val walkableAround = around
                    .count { it.type == LOOK_TERRAIN && it.terrain!!.isWalkable }

                val containersAround = around
                    .count { it.type == LOOK_STRUCTURES && it.structure!!.structureType == STRUCTURE_CONTAINER }

                val openSides = walkableAround - containersAround

                this@GlobalUpdate.log.info("${container.id}: open sides = $openSides")
                initializeSemaphore(container.id, openSides)
            }
        }

        if (room.storage != null)
            initializeSemaphore(room.storage!!.id, 8)


    }
}