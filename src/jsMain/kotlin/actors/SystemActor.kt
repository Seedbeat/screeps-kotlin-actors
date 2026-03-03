package actors

import actor.Actor
import actor.ActorSystem
import actor.message.IMessage
import screeps.api.Game
import screeps.api.values
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class SystemActor(id: String) : Actor(id), ILogging by Logging<SystemActor>(LogLevel.INFO) {

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
        is Lifecycle.Bootstrap -> onBootstrap()
        is Lifecycle.Tick -> onTick(msg.time)
    }

    private suspend fun onTick(time: Int) {
        log.info("System tick: $time")

        // Тут позже будет:
        // 1. sync room actors
        // 2. send RoomCommand.OnTick(...)
    }

    private suspend fun onBootstrap() {
        log.info("System bootstrap")

        // Тут позже можно:
        // 1. создать стартовые акторы
        // 2. прогреть состояние

        syncGameObjectsWithActors()
        // TODO: sendTickMessages(setOf())
    }

    fun syncGameObjectsWithActors() {
        val activeActors = mutableSetOf<String>()

        Game.rooms.values
            .filter { room -> room.controller?.my == true }
            .forEach { room ->
                activeActors.add(room.name)

                if (!ActorSystem.contains(room.name)) {
                    ActorSystem.spawn(actorId = room.name, create = ::RoomActor)
                }
            }

        Game.spawns.values
            .filter { spawn -> spawn.my }
            .forEach { spawn ->
                activeActors.add(spawn.id)

                if (!ActorSystem.contains(spawn.id)) {
                    ActorSystem.spawn(actorId = spawn.id, create = ::SpawnActor)
                }
            }

        removeStaleActors(activeActors)
    }

    private fun removeStaleActors(activeActors: Set<String>) {
        ActorSystem.actors().keys.forEach { actorId ->

            val isStale = !activeActors.contains(actorId)

            if (isStale) {
                ActorSystem.remove(actorId)
                log.info("Removed stale actor: $actorId")
            }
        }
    }

    private fun sendTickMessages(actorsIds: Set<String>) {
        actorsIds.forEach { actorId ->
            sendTo(actorId, RoomCommand.OnTick(Game.time))
        }
    }
}