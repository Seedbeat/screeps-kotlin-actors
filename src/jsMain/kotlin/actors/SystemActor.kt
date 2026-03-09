package actors

import actor.Actor
import actor.ActorSystem
import actor.message.IMessage
import actors.SystemRequest.CountCreeps
import actors.SystemResponse.CountCreepsResponse
import actors.base.IChildrenMultiManager
import actors.base.Lifecycle
import actors.base.OwnedCreepsManager
import actors.base.OwnedRoomsManager
import memory.assignmentRoom
import memory.delete
import memory.homeRoom
import memory.role
import screeps.api.Game
import screeps.api.Memory
import screeps.api.get
import screeps.api.keys
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class SystemActor(id: String) : Actor(id),
    IChildrenMultiManager,
    ILogging by Logging<SystemActor>(LogLevel.INFO) {

    override val managers = mapOf(
        RoomActor::class.simpleName!! to OwnedRoomsManager(),
        CreepActor::class.simpleName!! to OwnedCreepsManager()
    )

    companion object {
        const val SYSTEM = "SYSTEM"

        fun init() {
            ActorSystem.spawn(SYSTEM, ::SystemActor)
            ActorSystem.send(SYSTEM, SYSTEM, Lifecycle.Bootstrap)
        }

        fun tick() {
            ActorSystem.send(SYSTEM, SYSTEM, Lifecycle.Tick(Game.time))
            ActorSystem.tick()
        }
    }

    override suspend fun run() {
        while (true) {
            val msg = receive<IMessage>()
            log.debug("[${msg.messageId}] from='${msg.from}' payload=${msg.payload}")

            try {
                when (val payload = msg.payload) {
                    is Lifecycle -> onLifecycle(payload)
                    is SystemRequest -> {
                        val response = processRequest(payload)
                        sendTo(msg.from, response, msg.messageId)
                    }
                    else -> log.warn("Unsupported payload for SystemActor: $payload")
                }
            } catch (exception: Exception) {
                log.error("Failed to process message: $msg", exception)
            }
        }
    }

    override fun onDestroy() {
        destroyChildren()
    }

    private suspend fun onLifecycle(msg: Lifecycle) = when (msg) {
        is Lifecycle.Tick -> onTick(msg)
        is Lifecycle.Bootstrap -> onBootstrap()
    }

    private suspend fun onTick(msg: Lifecycle.Tick) {
        log.info("System tick: ${msg.time}")

        syncChildren()
        cleanupStaleCreepMemory()
        backfillCreepAffiliation()
        broadcast(this, msg)
    }

    private suspend fun onBootstrap() {
        log.info("System bootstrap")

        syncChildren()
        cleanupStaleCreepMemory()
        backfillCreepAffiliation()
        broadcast(this, Lifecycle.Bootstrap)
    }

    private fun processRequest(msg: SystemRequest): SystemResponse<*> = when (msg) {
        is CountCreeps -> CountCreepsResponse(
            result = Game.creeps.keys.count { name ->
                val creep = Game.creeps[name] ?: return@count false
                (msg.homeRoom == null || creep.memory.homeRoom == msg.homeRoom) &&
                    (msg.currentRoom == null || creep.room.name == msg.currentRoom) &&
                    (msg.assignmentRoom == null || creep.memory.assignmentRoom == msg.assignmentRoom) &&
                    (msg.role == null || creep.memory.role == msg.role)
            }
        )
    }

    private fun cleanupStaleCreepMemory() {
        Memory.creeps.keys
            .filter { name -> Game.creeps[name] == null }
            .forEach { name ->
                log.info("Deleting stale creep memory: $name")
                Memory.creeps.delete(name)
            }
    }

    private fun backfillCreepAffiliation() {
        Game.creeps.keys.forEach { name ->
            val creep = Game.creeps[name] ?: return@forEach
            val creepMemory = creep.memory

            if (creepMemory.homeRoom.isBlank()) {
                creepMemory.homeRoom = creep.room.name
                log.warn("[Migration] Backfilled homeRoom for creep '$name' with '${creep.room.name}'")
            }

            if (creepMemory.assignmentRoom.isBlank()) {
                creepMemory.assignmentRoom = creepMemory.homeRoom
            }
        }
    }
}
