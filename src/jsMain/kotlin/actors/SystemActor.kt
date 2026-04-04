package actors

import actors.base.*
import memory.assignment
import memory.delete
import screeps.api.*
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import kotlin.reflect.safeCast

class SystemActor(id: String) :
    ActorBase<Unit, SystemCommand, SystemRequest<*>, SystemResponse<*>>(id),
    ActorBinding<Unit> by NoBinding,
    ChildrenMultiManager,
    ILogging by Logging<SystemActor>(LogLevel.INFO) {

    override val managers = mapOf(
        RoomActor::class.simpleName!! to OwnedRoomsManager(),
        CreepActor::class.simpleName!! to OwnedCreepsManager()
    )

    override suspend fun processLifecycle(msg: Lifecycle) = when (msg) {
        is Lifecycle.Tick -> onTick(msg)
        is Lifecycle.Bootstrap -> onBootstrap(msg)
    }

    private suspend fun onTick(msg: Lifecycle.Tick) {
        log.info("System tick: ${msg.time}")

        syncChildren()
        cleanupStaleCreepMemory()
        broadcast(this, msg)
    }

    private suspend fun onBootstrap(msg: Lifecycle.Bootstrap) {
        log.info("System bootstrap")

        syncChildren()
        cleanupStaleCreepMemory()
        broadcast(this, msg)
    }

    override suspend fun processCommand(msg: SystemCommand) = when (msg) {
        SystemCommand.Noop -> {}
    }

    override suspend fun processRequest(msg: SystemRequest<*>): SystemResponse<*> = when (msg) {
        is SystemRequest.Query.Creeps -> queryCreeps(msg)
        is SystemRequest.Query.CreepsByAssignment<*> -> queryCreepsByAssignment(msg)
    }

    private fun queryCreeps(
        msg: SystemRequest.Query.Creeps
    ) = SystemResponse.Query.CreepsResponse(result = queryCreepsBase(limit = msg.limit) { name, creep ->

        val assignment = creep.memory.assignment
        CreepStatus(name, creep, assignment)
            .takeIf { msg.predicate(creep, assignment) }
    })

    private fun <T : CreepAssignment> queryCreepsByAssignment(
        msg: SystemRequest.Query.CreepsByAssignment<T>
    ) = SystemResponse.Query.CreepsResponse(result = queryCreepsBase(limit = msg.limit) { name, creep ->

        msg.type.safeCast(creep.memory.assignment)
            ?.takeIf { assignment -> msg.predicate(creep, assignment) }
            ?.let { assignment -> CreepStatus(name, creep, assignment) }
    })

    private inline fun <R> queryCreepsBase(
        limit: Int? = null,
        transform: (name: String, creep: Creep) -> R?
    ): List<R> {
        val result = mutableListOf<R>()

        for (name in Game.creeps.keys) {
            if (limit != null && result.size >= limit) break

            val creep = Game.creeps[name] ?: continue
            val item = transform(name, creep) ?: continue

            result += item
        }

        return result
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
