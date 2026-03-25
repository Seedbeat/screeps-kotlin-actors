package scheduler.events

import memory.delete
import memory.lockedObjectId
import room.releaseResource
import scheduler.IEvent
import screeps.api.*
import screeps.utils.isNotEmpty
import utils.log.ILogging
import utils.log.Logging


object Cleanup : IEvent, ILogging by Logging<Cleanup>() {
    override fun execute() {
        Game.creeps.takeIf { it.isNotEmpty() }?.let { currentCreeps ->
            Memory.creeps.entries
                .filter { (name, _) -> currentCreeps[name] == null }
                .forEach { (name, memory) ->
                    log.info("Found outdated creep: $name")

                    memory.lockedObjectId?.let { id ->
                        Game.getObjectById<Identifiable>(id)
                            ?.unsafeCast<RoomObjectNotNull>()
                            ?.room
                            ?.releaseResource("Cleanup", id)
                            ?: Game.rooms.values.forEach { it.releaseResource("Cleanup fallback", id) }
                    }

                    log.info("deleting:", name)
                    Memory.creeps.delete(name)
                }
        }
    }
}