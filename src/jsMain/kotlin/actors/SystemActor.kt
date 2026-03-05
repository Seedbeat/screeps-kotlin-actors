package actors

import actor.Actor
import actor.ActorSystem
import actor.message.IMessage
import actors.base.IChildrenManager
import actors.base.Lifecycle
import actors.base.OwnedRoomsManager
import screeps.api.Game
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
        broadcast(this, msg)
    }

    private suspend fun onBootstrap() {
        log.info("System bootstrap")

        syncChildren()
        broadcast(this, Lifecycle.Bootstrap)
    }
}