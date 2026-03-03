package actors

import actor.Actor
import actor.ActorSystem
import actor.message.IMessage
import actor.message.IPayload
import actors.base.Lifecycle
import screeps.api.Game
import screeps.api.values
import screeps.utils.lazyPerTick
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

    private val ownedRoomsNames by lazyPerTick {
        Game.rooms.values.filter { room -> room.controller?.my == true }.map { room -> room.name }.toSet()
    }

    private suspend fun onLifecycle(msg: Lifecycle) = when (msg) {
        is Lifecycle.Bootstrap -> onBootstrap()
        is Lifecycle.Tick -> onTick(msg)
    }

    private suspend fun onTick(msg: Lifecycle.Tick) {
        log.info("System tick: ${msg.time}")

        syncGameObjectsWithActors()
        broadcast(msg)
    }

    private suspend fun onBootstrap() {
        log.info("System bootstrap")

        syncGameObjectsWithActors()
        broadcast(Lifecycle.Bootstrap)
    }

    fun syncGameObjectsWithActors() {

        val roomActorsIds = ActorSystem.actors().values
            .filterIsInstance<RoomActor>()
            .map { it.id }
            .toSet()

        val toCreate = ownedRoomsNames - roomActorsIds
        val toRemove = roomActorsIds - ownedRoomsNames

        toCreate.forEach { actorId -> ActorSystem.spawn(actorId, ::RoomActor) }
        toRemove.forEach { actorId -> ActorSystem.remove(actorId) }
    }

    private fun broadcast(payload: IPayload) {
        ownedRoomsNames.forEach { actorId -> sendTo(actorId, payload) }
    }
}