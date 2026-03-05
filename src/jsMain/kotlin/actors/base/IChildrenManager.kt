package actors.base

import actor.Actor
import actor.ActorSystem
import actor.message.IPayload
import actors.RoomActor
import actors.SpawnActor
import screeps.api.FIND_MY_SPAWNS
import screeps.api.Game
import screeps.api.Room
import screeps.api.values
import screeps.utils.lazyPerTick

interface IChildrenManager<T : Actor> {
    val childrenIds: Set<String>
    fun filterChildren(actors: Collection<Actor>): Collection<Actor>

    val create: (String) -> T

    fun syncChildren() {
        val roomActorsIds = ActorSystem.actors().values
            .let(::filterChildren)
            .map(Actor::id)
            .toSet()

        val toCreate = childrenIds - roomActorsIds
        val toRemove = roomActorsIds - childrenIds

        toCreate.forEach { actorId -> ActorSystem.spawn(actorId, create) }
        toRemove.forEach { actorId -> ActorSystem.remove(actorId) }
    }

    fun broadcast(self: Actor, payload: IPayload) =
        childrenIds.forEach { actorId -> self.sendTo(actorId, payload) }
}

class OwnedRoomsManager : IChildrenManager<RoomActor> {
    override val childrenIds by lazyPerTick {
        Game.rooms.values.filter { room -> room.controller?.my == true }.map { room -> room.name }.toSet()
    }

    override val create = ::RoomActor

    override fun filterChildren(actors: Collection<Actor>) =
        actors.filterIsInstance<RoomActor>()
}

class RoomSpawnsManager(room: Room) : IChildrenManager<SpawnActor> {
    override val childrenIds by lazyPerTick {
        room.find(FIND_MY_SPAWNS).map { it.id }.toSet()
    }

    override val create = ::SpawnActor

    override fun filterChildren(actors: Collection<Actor>) =
        actors.filterIsInstance<RoomActor>()
}