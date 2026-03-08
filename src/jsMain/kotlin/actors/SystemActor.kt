package actors

import actor.Actor
import actor.ActorSystem
import actor.message.IMessage
import actors.base.IChildrenManager
import actors.base.Lifecycle
import actors.base.OwnedRoomsManager
import memory.delete
import screeps.api.Game
import screeps.api.Memory
import screeps.api.get
import screeps.api.keys
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class SystemActor(id: String) : Actor(id),
    IChildrenManager<RoomActor> by OwnedRoomsManager(),
    ILogging by Logging<SystemActor>(LogLevel.INFO) {

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
                    else -> log.warn("Unsupported payload for SystemActor: $payload")
                }
            } catch (exception: Exception) {
                log.error("Failed to process message: $msg", exception)
            }
        }
    }

    private suspend fun onLifecycle(msg: Lifecycle) = when (msg) {
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

    private fun cleanupStaleCreepMemory() {
        Memory.creeps.keys
            .filter { name -> Game.creeps[name] == null }
            .forEach { name ->
                log.info("Deleting stale creep memory: $name")
                Memory.creeps.delete(name)
            }
    }
}
