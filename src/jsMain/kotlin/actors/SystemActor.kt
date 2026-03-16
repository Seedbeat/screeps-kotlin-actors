package actors

import actors.SystemRequest.CountCreeps
import actors.SystemRequest.QueryCreeps
import actors.SystemResponse.CountCreepsResponse
import actors.SystemResponse.QueryCreepsResponse
import actors.base.*
import memory.*
import screeps.api.Game
import screeps.api.Memory
import screeps.api.get
import screeps.api.keys
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class SystemActor(id: String) :
    ActorBase<Unit, SystemCommand, SystemRequest, SystemResponse<*>>(id),
    ActorBinding<Unit> by NoBinding,
    ChildrenMultiManager,
    ILogging by Logging<SystemActor>(LogLevel.INFO) {

    override val managers = mapOf(
        RoomActor::class.simpleName!! to OwnedRoomsManager(),
        CreepActor::class.simpleName!! to OwnedCreepsManager()
    )

    override suspend fun processLifecycle(msg: Lifecycle) = when (msg) {
        is Lifecycle.Tick -> onTick(msg)
        is Lifecycle.Bootstrap -> onBootstrap()
    }

    private suspend fun onTick(msg: Lifecycle.Tick) {
        log.info("System tick: ${msg.time}")

        syncChildren()
        cleanupStaleCreepMemory()
        broadcast(this, msg)
    }

    private suspend fun onBootstrap() {
        log.info("System bootstrap")

        syncChildren()
        cleanupStaleCreepMemory()
        broadcast(this, Lifecycle.Bootstrap)
    }

    override suspend fun processCommand(msg: SystemCommand) = when (msg) {
        SystemCommand.Noop -> {}
    }

    override suspend fun processRequest(msg: SystemRequest): SystemResponse<*> = when (msg) {
        is CountCreeps -> CountCreepsResponse(
            result = Game.creeps.keys.count { name ->
                val creep = Game.creeps[name]
                    ?: return@count false

                (msg.homeRoom == null || creep.memory.homeRoom == msg.homeRoom) &&
                        (msg.currentRoom == null || creep.room.name == msg.currentRoom) &&
                        (msg.assignmentRoom == null || creep.memory.assignment.roomName == msg.assignmentRoom) &&
                        (msg.role == null || creep.memory.role == msg.role)
            }
        )
        is QueryCreeps -> QueryCreepsResponse(
            result = Game.creeps.keys.mapNotNull { name ->
                val creep = Game.creeps[name]
                    ?: return@mapNotNull null

                if (msg.homeRoom != null && creep.memory.homeRoom != msg.homeRoom) {
                    return@mapNotNull null
                }
                if (msg.currentRoom != null && creep.room.name != msg.currentRoom) {
                    return@mapNotNull null
                }
                val assignment = creep.memory.assignment.read()
                if (msg.assignmentRoom != null && assignment?.roomName != msg.assignmentRoom) {
                    return@mapNotNull null
                }

                CreepStatus(
                    actorId = name,
                    homeRoom = creep.memory.homeRoom,
                    currentRoom = creep.room.name,
                    assignment = assignment,
                    capabilities = CreepCapabilities.from(creep),
                    lockedResourceId = creep.memory.lockedObjectId.takeIf { it.isNotBlank() }
                )
            }
        )
    }

    override fun onDestroy() {
        destroyChildren()
    }

    private fun cleanupStaleCreepMemory() {
        Memory.creeps.keys
            .filter { name -> Game.creeps[name] == null }
            .forEach { name ->
                log.info("Deleting stale creep memory: $name")
                Memory.creeps.delete(name)
            }
    }
}
