package actors.base

import actor.Actor
import actor.ActorSystem
import actor.message.Payload
import actors.CreepActor
import actors.RoomActor
import actors.SpawnActor
import screeps.api.*
import utils.lazyPerTick

interface ChildrenManager<T : Actor> {
    val childrenIds: Set<String>
    val ownedChildrenIds: MutableSet<String>
    val create: (String) -> T

    fun syncChildren() {
        val desiredChildrenIds = childrenIds

        desiredChildrenIds
            .filter(ActorSystem::contains)
            .forEach(ownedChildrenIds::add)

        val toCreate = desiredChildrenIds
            .filterNot(ActorSystem::contains)
            .toSet()
        val toRemove = ownedChildrenIds - desiredChildrenIds

        toCreate.forEach { actorId ->
            ActorSystem.spawn(actorId, create)
            ownedChildrenIds.add(actorId)
        }
        toRemove.forEach { actorId ->
            ActorSystem.remove(actorId)
            ownedChildrenIds.remove(actorId)
        }
    }

    fun destroyChildren() {
        ownedChildrenIds.forEach { actorId -> ActorSystem.remove(actorId) }
        ownedChildrenIds.clear()
    }

    fun broadcast(self: Actor, payload: Payload) =
        childrenIds.forEach { actorId -> self.sendTo(actorId, payload) }
}

sealed class BaseChildrenManager<T : Actor> : ChildrenManager<T> {
    override val ownedChildrenIds = mutableSetOf<String>()
}

class OwnedRoomsManager : BaseChildrenManager<RoomActor>() {
    override val childrenIds by lazyPerTick {
        Game.rooms.values.filter { room -> room.controller?.my == true }.map { room -> room.name }.toSet()
    }
    override val create = ::RoomActor
}

class OwnedCreepsManager : BaseChildrenManager<CreepActor>() {
    override val childrenIds by lazyPerTick {
        Game.creeps.keys.toSet()
    }
    override val create = ::CreepActor
}

class RoomSpawnsManager(room: Room) : BaseChildrenManager<SpawnActor>() {
    override val childrenIds by lazyPerTick {
        room.find(FIND_MY_SPAWNS).map { it.id }.toSet()
    }
    override val create = ::SpawnActor
}
